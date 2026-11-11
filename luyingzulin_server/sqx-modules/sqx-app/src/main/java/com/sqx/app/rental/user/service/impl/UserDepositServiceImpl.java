package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.user.constants.UserDepositConstant;
import com.sqx.app.rental.user.domain.bo.*;
import com.sqx.app.rental.user.domain.entity.UserDeposit;
import com.sqx.app.rental.user.domain.entity.UserDepositLog;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositRefundVo;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositVo;
import com.sqx.app.rental.user.domain.vo.UserDepositLogVo;
import com.sqx.app.rental.user.domain.vo.UserDepositPayStatusVo;
import com.sqx.app.rental.user.domain.vo.UserDepositSummaryVo;
import com.sqx.app.rental.user.enums.DepositLogType;
import com.sqx.app.rental.user.mapper.UserDepositLogMapper;
import com.sqx.app.rental.user.mapper.UserDepositMapper;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.system.db.entity.SysConfigEntity;
import com.sqx.system.db.service.ISysConfigService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserDepositServiceImpl implements UserDepositService {

    @Resource
    private UserDepositMapper userDepositMapper;
    @Resource
    private UserDepositLogMapper userDepositLogMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private RentalWalletFlowService rentalWalletFlowService;
    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private ISysConfigService iSysConfigService;

    @Override
    public UserDepositSummaryVo summary(Long userId) {
        UserDeposit deposit = ensureDepositAccount(userId);
        UserDepositSummaryVo vo = new UserDepositSummaryVo();
        BigDecimal total = nullToZero(deposit.getTotalAmount());
        BigDecimal frozen = nullToZero(deposit.getFrozenAmount());
        BigDecimal available = total.subtract(frozen).max(BigDecimal.ZERO);
        BigDecimal pendingRefundAmount = sumPendingRefundAmount(userId);
        BigDecimal minDepositBalance = getMinDepositBalance();
        vo.setTotalAmount(total);
        vo.setFrozenAmount(frozen);
        vo.setAvailableAmount(available);
        vo.setStatus(deposit.getStatus());
        vo.setPendingRefundAmount(pendingRefundAmount);
        vo.setRefundPending(pendingRefundAmount.compareTo(BigDecimal.ZERO) > 0);
        vo.setCanRefund(!vo.getRefundPending() && available.compareTo(BigDecimal.ZERO) > 0);
        vo.setMinDepositBalance(minDepositBalance);
        return vo;
    }

    @Override
    public IPage<UserDepositLogVo> logPage(Long userId, UserDepositLogPageBo pageBo) {
        Page<UserDepositLog> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<UserDepositLog> wrapper = new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getUserId, userId)
                .eq(StringUtils.isNotBlank(pageBo.getLogType()), UserDepositLog::getLogType, pageBo.getLogType())
                .orderByDesc(UserDepositLog::getCreateTime);
        if (pageBo.getOrderId() != null) {
            RentOrder order = rentOrderMapper.selectById(pageBo.getOrderId());
            if (order == null) {
                return PageUtil.copyPage(page, Collections.emptyList());
            }
            wrapper.eq(UserDepositLog::getOrderNo, order.getOrderNo());
        }
        IPage<UserDepositLog> logPage = userDepositLogMapper.selectPage(page, wrapper);
        List<UserDepositLogVo> records = buildLogVoList(logPage.getRecords());
        return PageUtil.copyPage(logPage, records);
    }

    @Override
    public String applyPay(Long userId, UserDepositPayBo payBo) {
        String payNo = "DP" + GenUtil.genOrderNo();
        writeLog(userId, DepositLogType.PAY, payBo.getAmount(), payNo, "pending",
                StringUtils.defaultIfBlank(payBo.getRemark(), "押金缴纳待支付"));
        return payNo;
    }

    @Override
    public BigDecimal preparePay(Long userId, String payNo, BigDecimal amount) {
        UserDepositLog pending = requirePendingPayLog(userId, payNo);
        if (amount != null && pending.getAmount().compareTo(amount) != 0) {
            throw new BizException("押金支付金额不一致");
        }
        return pending.getAmount();
    }

    @Override
    public UserDepositPayStatusVo payStatus(Long userId, String payNo) {
        UserDepositLog log = userDepositLogMapper.selectOne(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getOrderNo, payNo)
                .eq(UserDepositLog::getUserId, userId)
                .eq(UserDepositLog::getLogType, DepositLogType.PAY.getCode())
                .last("LIMIT 1"));
        if (log == null) {
            throw new BizException("押金支付单不存在");
        }
        UserDepositPayStatusVo vo = new UserDepositPayStatusVo();
        vo.setPayNo(payNo);
        vo.setStatus(log.getStatus());
        vo.setPaid("completed".equals(log.getStatus()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long userId, UserDepositRefundApplyBo refundBo) {
        assertNoPendingRefund(userId);
        UserDeposit deposit = ensureDepositAccount(userId);
        BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
        if (refundBo.getAmount().compareTo(available) > 0) {
            throw new BizException("可退押金不足");
        }
        int affected = userDepositMapper.freezeAvailable(userId, refundBo.getAmount());
        if (affected <= 0) {
            throw new BizException("可退押金不足");
        }
        writeLog(userId, DepositLogType.REFUND, refundBo.getAmount(), null,
                "pending", StringUtils.defaultIfBlank(refundBo.getRemark(), "用户申请退押"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onDepositPaySuccess(Long userId, String payNo, BigDecimal amount) {
        UserDepositLog pending = userDepositLogMapper.selectOne(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getOrderNo, payNo)
                .eq(UserDepositLog::getStatus, "pending")
                .last("LIMIT 1"));
        if (pending == null) {
            return;
        }
        Long targetUserId = userId != null ? userId : pending.getUserId();
        BigDecimal payAmount = amount != null ? amount : pending.getAmount();
        ensureDepositAccount(targetUserId);
        userDepositMapper.incrTotal(targetUserId, payAmount);
        pending.setStatus("completed");
        pending.setRemark("押金缴纳成功");
        userDepositLogMapper.updateById(pending);
    }

    @Override
    public void assertSufficientForOrder(Long userId, BigDecimal requiredAmount) {
        if (requiredAmount == null || requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        UserDeposit deposit = ensureDepositAccount(userId);
        BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
        BigDecimal minBalance = getMinDepositBalance();
        BigDecimal threshold = requiredAmount.max(minBalance);
        if (available.compareTo(threshold) >= 0) {
            return;
        }
        if (minBalance.compareTo(BigDecimal.ZERO) > 0 && available.compareTo(minBalance) < 0) {
            throw new BizException("账户可用押金需不低于 ¥" + minBalance + "，请先补充押金");
        }
        throw new BizException("可用押金不足，请先缴纳押金");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeForOrder(Long userId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        ensureDepositAccount(userId);
        int affected = userDepositMapper.freezeAvailable(userId, amount);
        if (affected <= 0) {
            throw new BizException("可用押金不足，订单押金冻结失败");
        }
        writeLog(userId, DepositLogType.FREEZE, amount, orderNo, "completed", "订单押金冻结");
        rentalWalletFlowService.recordDepositFreeze(userId, null, orderNo, amount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeForCancelledOrder(Long userId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        userDepositMapper.decrFrozen(userId, amount);
        writeLog(userId, DepositLogType.UNFREEZE, amount, orderNo, "completed", "订单取消解冻押金");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundAfterInspection(Long userId, String orderNo, BigDecimal refundAmount, BigDecimal deductAmount) {
        BigDecimal refund = refundAmount == null ? BigDecimal.ZERO : refundAmount;
        BigDecimal deduct = deductAmount == null ? BigDecimal.ZERO : deductAmount;
        BigDecimal releaseAmount = refund.add(deduct);
        if (releaseAmount.compareTo(BigDecimal.ZERO) > 0) {
            userDepositMapper.decrFrozen(userId, releaseAmount);
            writeLog(userId, DepositLogType.UNFREEZE, releaseAmount, orderNo, "completed", "订单验收解冻押金");
        }
        if (deduct.compareTo(BigDecimal.ZERO) > 0) {
            writeLog(userId, DepositLogType.DEDUCT, deduct, orderNo, "completed", "验收扣款");
        }
        if (refund.compareTo(BigDecimal.ZERO) > 0) {
            userDepositMapper.decrTotal(userId, refund);
            writeLog(userId, DepositLogType.REFUND, refund, orderNo, "completed", "验收退押");
            rentalWalletFlowService.recordDepositRefund(userId, null, orderNo, refund);
        }
    }

    @Override
    public IPage<AdminUserDepositVo> adminPage(AdminUserDepositPageBo pageBo) {
        Page<UserDeposit> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<UserDeposit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(pageBo.getStatus() != null, UserDeposit::getStatus, pageBo.getStatus())
                .eq(pageBo.getUserId() != null, UserDeposit::getUserId, pageBo.getUserId())
                .orderByDesc(UserDeposit::getUpdateTime);
        if (StringUtils.isNotBlank(pageBo.getPhone())) {
            List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .like(UserEntity::getPhone, pageBo.getPhone()));
            if (CollectionUtils.isEmpty(users)) {
                return PageUtil.copyPage(page, Collections.emptyList());
            }
            List<Long> userIds = users.stream().map(UserEntity::getUserId).collect(Collectors.toList());
            wrapper.in(UserDeposit::getUserId, userIds);
        }
        IPage<UserDeposit> depositPage = userDepositMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(depositPage.getRecords())) {
            return PageUtil.copyPage(depositPage, Collections.emptyList());
        }
        List<Long> userIds = depositPage.getRecords().stream().map(UserDeposit::getUserId).collect(Collectors.toList());
        Map<Long, UserEntity> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
        List<AdminUserDepositVo> records = depositPage.getRecords().stream()
                .map(d -> toAdminVo(d, userMap.get(d.getUserId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(depositPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefund(AdminUserDepositRefundBo refundBo) {
        UserDeposit deposit = userDepositMapper.selectOne(new LambdaQueryWrapper<UserDeposit>()
                .eq(UserDeposit::getUserId, refundBo.getUserId()).last("LIMIT 1"));
        if (deposit == null) {
            throw new BizException("押金账户不存在");
        }
        BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
        if (refundBo.getAmount().compareTo(available) > 0) {
            throw new BizException("可退押金不足");
        }
        userDepositMapper.decrTotal(refundBo.getUserId(), refundBo.getAmount());
        writeLog(refundBo.getUserId(), DepositLogType.REFUND, refundBo.getAmount(), null,
                "completed", StringUtils.defaultIfBlank(refundBo.getRemark(), "运营人工退押"));
        rentalWalletFlowService.recordDepositRefund(refundBo.getUserId(), null, null, refundBo.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAdjust(AdminUserDepositAdjustBo adjustBo) {
        if (adjustBo.getDirection() == null || (adjustBo.getDirection() != 1 && adjustBo.getDirection() != 2)) {
            throw new BizException("调整方向无效");
        }
        ensureDepositAccount(adjustBo.getUserId());
        BigDecimal amount = adjustBo.getAmount();
        String remark = StringUtils.defaultIfBlank(adjustBo.getRemark(), "运营人工调整押金");
        if (Integer.valueOf(1).equals(adjustBo.getDirection())) {
            userDepositMapper.incrTotal(adjustBo.getUserId(), amount);
            writeLog(adjustBo.getUserId(), DepositLogType.ADJUST, amount, null, "completed", remark + "（增加）");
            return;
        }
        UserDeposit deposit = userDepositMapper.selectOne(new LambdaQueryWrapper<UserDeposit>()
                .eq(UserDeposit::getUserId, adjustBo.getUserId()).last("LIMIT 1"));
        BigDecimal available = nullToZero(deposit.getTotalAmount()).subtract(nullToZero(deposit.getFrozenAmount()));
        if (amount.compareTo(available) > 0) {
            throw new BizException("可减少押金不能超过可用押金");
        }
        userDepositMapper.decrTotal(adjustBo.getUserId(), amount);
        writeLog(adjustBo.getUserId(), DepositLogType.ADJUST, amount, null, "completed", remark + "（减少）");
    }

    @Override
    public IPage<AdminUserDepositRefundVo> adminRefundPage(AdminUserDepositRefundPageBo pageBo) {
        Page<UserDepositLog> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<UserDepositLog> wrapper = new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getLogType, DepositLogType.REFUND.getCode())
                .eq(pageBo.getUserId() != null, UserDepositLog::getUserId, pageBo.getUserId())
                .eq(StringUtils.isNotBlank(pageBo.getStatus()), UserDepositLog::getStatus, pageBo.getStatus())
                .orderByDesc(UserDepositLog::getCreateTime);
        IPage<UserDepositLog> logPage = userDepositLogMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(logPage.getRecords())) {
            return PageUtil.copyPage(logPage, Collections.emptyList());
        }
        List<Long> userIds = logPage.getRecords().stream()
                .map(UserDepositLog::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserEntity> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
        List<AdminUserDepositRefundVo> records = logPage.getRecords().stream()
                .map(log -> toAdminRefundVo(log, userMap.get(log.getUserId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(logPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminAuditRefund(AdminUserDepositRefundAuditBo auditBo) {
        UserDepositLog pending = userDepositLogMapper.selectById(auditBo.getLogId());
        if (pending == null
                || !DepositLogType.REFUND.getCode().equals(pending.getLogType())
                || !"pending".equals(pending.getStatus())) {
            throw new BizException("退押申请不存在或已处理");
        }
        boolean approve = "approve".equalsIgnoreCase(StringUtils.trimToEmpty(auditBo.getAction()));
        boolean reject = "reject".equalsIgnoreCase(StringUtils.trimToEmpty(auditBo.getAction()));
        if (!approve && !reject) {
            throw new BizException("不支持的审核动作");
        }
        Long userId = pending.getUserId();
        BigDecimal amount = pending.getAmount();
        if (approve) {
            int affected = userDepositMapper.refundFrozen(userId, amount);
            if (affected <= 0) {
                throw new BizException("押金扣减失败，请核对账户冻结金额");
            }
            pending.setStatus("completed");
            pending.setRemark(StringUtils.defaultIfBlank(auditBo.getRemark(), "平台已通过退押申请"));
            rentalWalletFlowService.recordDepositRefund(userId, null, null, amount);
        } else {
            int affected = userDepositMapper.unfreezeAvailable(userId, amount);
            if (affected <= 0) {
                throw new BizException("押金解冻失败，请核对账户状态");
            }
            pending.setStatus("rejected");
            pending.setRemark(StringUtils.defaultIfBlank(auditBo.getRemark(), "平台已驳回退押申请"));
        }
        userDepositLogMapper.updateById(pending);
    }

    @Override
    public int countPendingRefund() {
        Long count = userDepositLogMapper.selectCount(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getLogType, DepositLogType.REFUND.getCode())
                .eq(UserDepositLog::getStatus, "pending"));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private UserDeposit ensureDepositAccount(Long userId) {
        UserDeposit deposit = userDepositMapper.selectOne(new LambdaQueryWrapper<UserDeposit>()
                .eq(UserDeposit::getUserId, userId).last("LIMIT 1"));
        if (deposit == null) {
            deposit = new UserDeposit();
            deposit.setUserId(userId);
            deposit.setTotalAmount(BigDecimal.ZERO);
            deposit.setFrozenAmount(BigDecimal.ZERO);
            deposit.setStatus(1);
            userDepositMapper.insert(deposit);
        }
        return deposit;
    }

    private UserDepositLog requirePendingPayLog(Long userId, String payNo) {
        UserDepositLog pending = userDepositLogMapper.selectOne(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getOrderNo, payNo)
                .eq(UserDepositLog::getUserId, userId)
                .eq(UserDepositLog::getLogType, DepositLogType.PAY.getCode())
                .eq(UserDepositLog::getStatus, "pending")
                .last("LIMIT 1"));
        if (pending == null) {
            throw new BizException("押金支付单不存在或已支付");
        }
        return pending;
    }

    private void writeLog(Long userId, DepositLogType logType, BigDecimal amount,
                          String orderNo, String status, String remark) {
        UserDepositLog log = new UserDepositLog();
        log.setLogNo("DL" + GenUtil.genOrderNo());
        log.setUserId(userId);
        log.setLogType(logType.getCode());
        log.setAmount(amount);
        log.setOrderNo(orderNo);
        log.setStatus(status);
        log.setRemark(remark);
        userDepositLogMapper.insert(log);
    }

    private UserDepositLogVo toLogVo(UserDepositLog log, Map<String, Long> orderNoToId) {
        UserDepositLogVo vo = new UserDepositLogVo();
        vo.setId(log.getId());
        vo.setLogNo(log.getLogNo());
        vo.setLogType(log.getLogType());
        try {
            vo.setLogTypeLabel(DepositLogType.fromCode(log.getLogType()).getLabel());
        } catch (Exception e) {
            vo.setLogTypeLabel(log.getLogType());
        }
        vo.setAmount(log.getAmount());
        vo.setOrderNo(log.getOrderNo());
        if (StringUtils.isNotBlank(log.getOrderNo()) && orderNoToId != null) {
            vo.setOrderId(orderNoToId.get(log.getOrderNo()));
        }
        vo.setStatus(log.getStatus());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private List<UserDepositLogVo> buildLogVoList(List<UserDepositLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }
        List<String> orderNos = logs.stream()
                .map(UserDepositLog::getOrderNo)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Long> orderNoToId = Collections.emptyMap();
        if (CollectionUtils.isNotEmpty(orderNos)) {
            orderNoToId = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>()
                            .in(RentOrder::getOrderNo, orderNos)
                            .select(RentOrder::getId, RentOrder::getOrderNo))
                    .stream()
                    .collect(Collectors.toMap(RentOrder::getOrderNo, RentOrder::getId, (a, b) -> a));
        }
        Map<String, Long> finalOrderNoToId = orderNoToId;
        return logs.stream()
                .map(log -> toLogVo(log, finalOrderNoToId))
                .collect(Collectors.toList());
    }

    private AdminUserDepositVo toAdminVo(UserDeposit deposit, UserEntity user) {
        AdminUserDepositVo vo = new AdminUserDepositVo();
        vo.setId(deposit.getId());
        vo.setUserId(deposit.getUserId());
        vo.setTotalAmount(nullToZero(deposit.getTotalAmount()));
        vo.setFrozenAmount(nullToZero(deposit.getFrozenAmount()));
        vo.setAvailableAmount(vo.getTotalAmount().subtract(vo.getFrozenAmount()).max(BigDecimal.ZERO));
        vo.setStatus(deposit.getStatus());
        if (user != null) {
            vo.setPhoneMasked(RentalUserSupport.maskPhone(user.getPhone()));
            vo.setPhone(user.getPhone());
            vo.setUserName(user.getUserName());
        }
        return vo;
    }

    private AdminUserDepositRefundVo toAdminRefundVo(UserDepositLog log, UserEntity user) {
        AdminUserDepositRefundVo vo = new AdminUserDepositRefundVo();
        vo.setId(log.getId());
        vo.setLogNo(log.getLogNo());
        vo.setUserId(log.getUserId());
        vo.setAmount(log.getAmount());
        vo.setStatus(log.getStatus());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setPhone(user.getPhone());
            vo.setPhoneMasked(RentalUserSupport.maskPhone(user.getPhone()));
        }
        return vo;
    }

    private BigDecimal sumPendingRefundAmount(Long userId) {
        List<UserDepositLog> pendingLogs = userDepositLogMapper.selectList(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getUserId, userId)
                .eq(UserDepositLog::getLogType, DepositLogType.REFUND.getCode())
                .eq(UserDepositLog::getStatus, "pending"));
        if (pendingLogs.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return pendingLogs.stream()
                .map(UserDepositLog::getAmount)
                .filter(item -> item != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void assertNoPendingRefund(Long userId) {
        Long pendingCount = userDepositLogMapper.selectCount(new LambdaQueryWrapper<UserDepositLog>()
                .eq(UserDepositLog::getUserId, userId)
                .eq(UserDepositLog::getLogType, DepositLogType.REFUND.getCode())
                .eq(UserDepositLog::getStatus, "pending"));
        if (pendingCount != null && pendingCount > 0) {
            throw new BizException("已有退押申请审核中，请等待平台处理");
        }
    }

    private BigDecimal getMinDepositBalance() {
        SysConfigEntity config = iSysConfigService.getOne(new LambdaQueryWrapper<SysConfigEntity>()
                .eq(SysConfigEntity::getParamKey, UserDepositConstant.config.USER_MIN_DEPOSIT_BALANCE)
                .last("LIMIT 1"));
        if (config == null || StringUtils.isBlank(config.getParamValue())) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(config.getParamValue().trim()).max(BigDecimal.ZERO);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
