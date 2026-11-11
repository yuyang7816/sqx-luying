package com.sqx.app.merchant.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.merchant.constants.MerchantConstant;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositAdjustBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositLogPageBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositPayBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundApplyBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundAuditBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundPageBo;
import com.sqx.app.merchant.wallet.domain.entity.MerchantDeposit;
import com.sqx.app.merchant.wallet.domain.entity.MerchantDepositLog;
import com.sqx.app.merchant.wallet.domain.vo.AdminMerchantSecurityDepositRefundVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositLogVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositPayStatusVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositSummaryVo;
import com.sqx.app.merchant.wallet.enums.MerchantDepositLogType;
import com.sqx.app.merchant.wallet.mapper.MerchantDepositLogMapper;
import com.sqx.app.merchant.wallet.mapper.MerchantDepositMapper;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.system.db.entity.SysConfigEntity;
import com.sqx.system.db.service.ISysConfigService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MerchantSecurityDepositServiceImpl implements MerchantSecurityDepositService {

    @Resource
    private MerchantDepositMapper merchantDepositMapper;
    @Resource
    private MerchantDepositLogMapper merchantDepositLogMapper;
    @Resource
    private ISysConfigService iSysConfigService;
    @Resource
    private MerchantMapper merchantMapper;

    @Override
    public MerchantSecurityDepositSummaryVo summary(Long merchantId) {
        MerchantDeposit deposit = ensureAccount(merchantId);
        MerchantSecurityDepositSummaryVo vo = new MerchantSecurityDepositSummaryVo();
        enrichSummary(vo, deposit);
        return vo;
    }

    @Override
    public IPage<MerchantSecurityDepositLogVo> logPage(Long merchantId, MerchantSecurityDepositLogPageBo pageBo) {
        Page<MerchantDepositLog> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        IPage<MerchantDepositLog> logPage = merchantDepositLogMapper.selectPage(page, new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .and(wrapper -> wrapper.ne(MerchantDepositLog::getLogType, MerchantDepositLogType.PAY.getCode())
                        .or().eq(MerchantDepositLog::getStatus, "completed"))
                .orderByDesc(MerchantDepositLog::getCreateTime));
        List<MerchantSecurityDepositLogVo> records = logPage.getRecords().stream()
                .map(this::toLogVo)
                .collect(Collectors.toList());
        return PageUtil.copyPage(logPage, records);
    }

    @Override
    public String applyPay(Long merchantId, MerchantSecurityDepositPayBo payBo) {
        BigDecimal required = getRequiredAmount();
        if (required.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("当前无需缴纳商户保证金");
        }
        MerchantDeposit deposit = ensureAccount(merchantId);
        BigDecimal needPay = calcNeedPayAmount(deposit, required);
        if (needPay.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("商户保证金已缴纳完成");
        }
        closeStalePendingPayLogs(merchantId);
        String payNo = "MDP" + GenUtil.genOrderNo();
        writeLog(merchantId, MerchantDepositLogType.PAY, needPay, payNo, "pending",
                StringUtils.defaultIfBlank(payBo.getRemark(), "商户保证金缴纳待支付"));
        return payNo;
    }

    @Override
    public BigDecimal preparePay(Long merchantId, String payNo, BigDecimal amount) {
        MerchantDepositLog pending = merchantDepositLogMapper.selectOne(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getOrderNo, payNo)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.PAY.getCode())
                .eq(MerchantDepositLog::getStatus, "pending")
                .last("LIMIT 1"));
        if (pending == null) {
            throw new BizException("保证金支付单不存在或已处理");
        }
        if (merchantId != null && !merchantId.equals(pending.getMerchantId())) {
            throw new BizException("保证金支付单不存在");
        }
        if (amount != null && pending.getAmount().compareTo(amount) != 0) {
            throw new BizException("保证金支付金额不一致");
        }
        return pending.getAmount();
    }

    @Override
    public MerchantSecurityDepositPayStatusVo payStatus(Long merchantId, String payNo) {
        MerchantDepositLog log = merchantDepositLogMapper.selectOne(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getOrderNo, payNo)
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.PAY.getCode())
                .last("LIMIT 1"));
        if (log == null) {
            throw new BizException("保证金支付单不存在");
        }
        MerchantSecurityDepositPayStatusVo vo = new MerchantSecurityDepositPayStatusVo();
        vo.setPayNo(payNo);
        vo.setStatus(log.getStatus());
        vo.setPaid("completed".equals(log.getStatus()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long merchantId, MerchantSecurityDepositRefundApplyBo refundBo) {
        assertNoPendingRefund(merchantId);
        MerchantDeposit deposit = ensureAccount(merchantId);
        BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("暂无可退保证金");
        }
        if (refundBo.getAmount().compareTo(available) > 0) {
            throw new BizException("可退保证金不足");
        }
        int affected = merchantDepositMapper.freezeAvailable(merchantId, refundBo.getAmount());
        if (affected <= 0) {
            throw new BizException("可退保证金不足");
        }
        writeLog(merchantId, MerchantDepositLogType.REFUND, refundBo.getAmount(), null,
                "pending", StringUtils.defaultIfBlank(refundBo.getRemark(), "商户申请退还保证金"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onPaySuccess(Long merchantId, String payNo, BigDecimal amount) {
        MerchantDepositLog pending = merchantDepositLogMapper.selectOne(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getOrderNo, payNo)
                .eq(MerchantDepositLog::getStatus, "pending")
                .last("LIMIT 1"));
        if (pending == null) {
            return;
        }
        Long targetMerchantId = merchantId != null ? merchantId : pending.getMerchantId();
        BigDecimal payAmount = amount != null ? amount : pending.getAmount();
        ensureAccount(targetMerchantId);
        merchantDepositMapper.incrTotal(targetMerchantId, payAmount);
        pending.setStatus("completed");
        pending.setRemark("商户保证金缴纳成功");
        merchantDepositLogMapper.updateById(pending);
    }

    @Override
    public BigDecimal getRequiredAmount() {
        String value = resolveSecurityDepositConfigValue();
        if (StringUtils.isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim()).max(BigDecimal.ZERO);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 保证金金额以数据库为准，避免 Redis 缓存未及时刷新导致读到旧值 0。
     */
    private String resolveSecurityDepositConfigValue() {
        SysConfigEntity config = iSysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getParamKey, MerchantConstant.securityDeposit.MERCHANT_SECURITY_DEPOSIT_AMOUNT)
                .last("LIMIT 1"));
        if (config == null || StringUtils.isBlank(config.getParamValue())) {
            return null;
        }
        if (!isConfigEnabled(config.getStatus())) {
            return null;
        }
        return config.getParamValue();
    }

    private boolean isConfigEnabled(Object status) {
        if (status == null) {
            return true;
        }
        String normalized = String.valueOf(status).trim();
        if (normalized.isEmpty()) {
            return true;
        }
        return !("0".equals(normalized) || "false".equalsIgnoreCase(normalized));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAdjust(MerchantSecurityDepositAdjustBo adjustBo) {
        BigDecimal amount = adjustBo.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("调整金额必须大于0");
        }
        boolean reduce = "reduce".equalsIgnoreCase(StringUtils.trimToEmpty(adjustBo.getDirection()));
        Long merchantId = adjustBo.getMerchantId();
        MerchantDeposit deposit = ensureAccount(merchantId);
        if (reduce) {
            BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
            if (available.compareTo(amount) < 0) {
                throw new BizException("可扣减保证金不足");
            }
            merchantDepositMapper.decrTotal(merchantId, amount);
        } else {
            merchantDepositMapper.incrTotal(merchantId, amount);
        }
        String defaultRemark = reduce ? "平台扣减保证金" : "平台增加保证金";
        writeLog(merchantId, MerchantDepositLogType.ADJUST, amount, null, "completed",
                StringUtils.defaultIfBlank(adjustBo.getRemark(), defaultRemark));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPendingPay(Long merchantId, String payNo) {
        if (StringUtils.isBlank(payNo)) {
            return;
        }
        MerchantDepositLog pending = merchantDepositLogMapper.selectOne(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getOrderNo, payNo)
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.PAY.getCode())
                .eq(MerchantDepositLog::getStatus, "pending")
                .last("LIMIT 1"));
        if (pending == null) {
            return;
        }
        pending.setStatus("cancelled");
        pending.setRemark("支付已取消");
        merchantDepositLogMapper.updateById(pending);
    }

    private void closeStalePendingPayLogs(Long merchantId) {
        List<MerchantDepositLog> staleLogs = merchantDepositLogMapper.selectList(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.PAY.getCode())
                .eq(MerchantDepositLog::getStatus, "pending"));
        if (staleLogs.isEmpty()) {
            return;
        }
        for (MerchantDepositLog log : staleLogs) {
            log.setStatus("cancelled");
            log.setRemark("支付已关闭");
            merchantDepositLogMapper.updateById(log);
        }
    }

    @Override
    public IPage<AdminMerchantSecurityDepositRefundVo> adminRefundPage(MerchantSecurityDepositRefundPageBo pageBo) {
        Page<MerchantDepositLog> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<MerchantDepositLog> wrapper = new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.REFUND.getCode())
                .eq(pageBo.getMerchantId() != null, MerchantDepositLog::getMerchantId, pageBo.getMerchantId())
                .eq(StringUtils.isNotBlank(pageBo.getStatus()), MerchantDepositLog::getStatus, pageBo.getStatus())
                .orderByDesc(MerchantDepositLog::getCreateTime);
        IPage<MerchantDepositLog> logPage = merchantDepositLogMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(logPage.getRecords())) {
            return PageUtil.copyPage(logPage, Collections.emptyList());
        }
        List<Long> merchantIds = logPage.getRecords().stream()
                .map(MerchantDepositLog::getMerchantId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Merchant> merchantMap = merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, item -> item, (a, b) -> a));
        List<AdminMerchantSecurityDepositRefundVo> records = logPage.getRecords().stream()
                .map(log -> toAdminRefundVo(log, merchantMap.get(log.getMerchantId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(logPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAuditRefund(MerchantSecurityDepositRefundAuditBo auditBo) {
        MerchantDepositLog pending = merchantDepositLogMapper.selectById(auditBo.getLogId());
        if (pending == null
                || !MerchantDepositLogType.REFUND.getCode().equals(pending.getLogType())
                || !"pending".equals(pending.getStatus())) {
            throw new BizException("退保申请不存在或已处理");
        }
        boolean approve = "approve".equalsIgnoreCase(StringUtils.trimToEmpty(auditBo.getAction()));
        boolean reject = "reject".equalsIgnoreCase(StringUtils.trimToEmpty(auditBo.getAction()));
        if (!approve && !reject) {
            throw new BizException("不支持的审核动作");
        }
        Long merchantId = pending.getMerchantId();
        BigDecimal amount = pending.getAmount();
        if (approve) {
            int affected = merchantDepositMapper.refundFrozen(merchantId, amount);
            if (affected <= 0) {
                throw new BizException("保证金扣减失败，请核对账户冻结金额");
            }
            pending.setStatus("completed");
            pending.setRemark(StringUtils.defaultIfBlank(auditBo.getRemark(), "平台已通过退保申请"));
        } else {
            int affected = merchantDepositMapper.unfreezeAvailable(merchantId, amount);
            if (affected <= 0) {
                throw new BizException("保证金解冻失败，请核对账户状态");
            }
            pending.setStatus("rejected");
            pending.setRemark(StringUtils.defaultIfBlank(auditBo.getRemark(), "平台已驳回退保申请"));
        }
        merchantDepositLogMapper.updateById(pending);
    }

    @Override
    public boolean isDepositRequired() {
        return getRequiredAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean isDepositPaid(Long merchantId) {
        if (!isDepositRequired()) {
            return true;
        }
        MerchantDeposit deposit = ensureAccount(merchantId);
        return nullToZero(deposit.getTotalAmount()).compareTo(getRequiredAmount()) >= 0;
    }

    @Override
    public void assertDepositPaidIfRequired(Long merchantId) {
        if (isDepositPaid(merchantId)) {
            return;
        }
        throw new BizException("请先缴纳商户保证金后再操作");
    }

    private void enrichSummary(MerchantSecurityDepositSummaryVo vo, MerchantDeposit deposit) {
        BigDecimal required = getRequiredAmount();
        BigDecimal total = nullToZero(deposit.getTotalAmount());
        BigDecimal frozen = nullToZero(deposit.getFrozenAmount());
        BigDecimal available = total.subtract(frozen).max(BigDecimal.ZERO);
        boolean depositRequired = required.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal needPay = depositRequired ? calcNeedPayAmount(deposit, required) : BigDecimal.ZERO;
        BigDecimal pendingRefundAmount = sumPendingRefundAmount(deposit.getMerchantId());
        boolean refundPending = pendingRefundAmount.compareTo(BigDecimal.ZERO) > 0;

        vo.setTotalAmount(total);
        vo.setFrozenAmount(frozen);
        vo.setAvailableAmount(available);
        vo.setStatus(deposit.getStatus());
        vo.setRequiredAmount(required);
        vo.setNeedPayAmount(needPay);
        vo.setDepositRequired(depositRequired);
        vo.setDepositPaid(!depositRequired || total.compareTo(required) >= 0);
        vo.setRefundPending(refundPending);
        vo.setPendingRefundAmount(pendingRefundAmount);
        vo.setCanRefund(!refundPending && available.compareTo(BigDecimal.ZERO) > 0);
    }

    private BigDecimal sumPendingRefundAmount(Long merchantId) {
        List<MerchantDepositLog> pendingLogs = merchantDepositLogMapper.selectList(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.REFUND.getCode())
                .eq(MerchantDepositLog::getStatus, "pending"));
        if (pendingLogs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return pendingLogs.stream()
                .map(MerchantDepositLog::getAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertNoPendingRefund(Long merchantId) {
        Long pendingCount = merchantDepositLogMapper.selectCount(new LambdaQueryWrapper<MerchantDepositLog>()
                .eq(MerchantDepositLog::getMerchantId, merchantId)
                .eq(MerchantDepositLog::getLogType, MerchantDepositLogType.REFUND.getCode())
                .eq(MerchantDepositLog::getStatus, "pending"));
        if (pendingCount != null && pendingCount > 0) {
            throw new BizException("已有退保申请审核中，请等待平台处理");
        }
    }

    private AdminMerchantSecurityDepositRefundVo toAdminRefundVo(MerchantDepositLog log, Merchant merchant) {
        AdminMerchantSecurityDepositRefundVo vo = new AdminMerchantSecurityDepositRefundVo();
        vo.setId(log.getId());
        vo.setLogNo(log.getLogNo());
        vo.setMerchantId(log.getMerchantId());
        vo.setMerchantName(merchant != null ? merchant.getName() : "");
        vo.setContactPhone(merchant != null ? merchant.getContactPhone() : "");
        vo.setAmount(log.getAmount());
        vo.setStatus(log.getStatus());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private BigDecimal calcNeedPayAmount(MerchantDeposit deposit, BigDecimal required) {
        return required.subtract(nullToZero(deposit.getTotalAmount())).max(BigDecimal.ZERO);
    }

    private MerchantDeposit ensureAccount(Long merchantId) {
        MerchantDeposit deposit = merchantDepositMapper.selectOne(new LambdaQueryWrapper<MerchantDeposit>()
                .eq(MerchantDeposit::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (deposit != null) {
            return deposit;
        }
        deposit = new MerchantDeposit();
        deposit.setMerchantId(merchantId);
        deposit.setTotalAmount(BigDecimal.ZERO);
        deposit.setFrozenAmount(BigDecimal.ZERO);
        deposit.setStatus(1);
        merchantDepositMapper.insert(deposit);
        return deposit;
    }

    private void writeLog(Long merchantId, MerchantDepositLogType logType, BigDecimal amount,
                          String orderNo, String status, String remark) {
        MerchantDepositLog log = new MerchantDepositLog();
        log.setLogNo("MDL" + GenUtil.genOrderNo());
        log.setMerchantId(merchantId);
        log.setLogType(logType.getCode());
        log.setAmount(amount);
        log.setOrderNo(orderNo);
        log.setStatus(status);
        log.setRemark(remark);
        merchantDepositLogMapper.insert(log);
    }

    private MerchantSecurityDepositLogVo toLogVo(MerchantDepositLog log) {
        MerchantSecurityDepositLogVo vo = new MerchantSecurityDepositLogVo();
        BeanUtils.copyProperties(log, vo);
        vo.setLogTypeLabel(MerchantDepositLogType.fromCode(log.getLogType()).getLabel());
        return vo;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
