package com.sqx.app.referral.withdraw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.referral.commission.db.service.IReferralCommissionService;
import com.sqx.app.referral.constants.ReferralConstant;
import com.sqx.app.referral.user.db.service.IReferralUserCommissionService;
import com.sqx.app.referral.user.domain.entity.ReferralUserCommission;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.wallet.service.MerchantWalletAccountService;
import com.sqx.app.referral.withdraw.db.service.IWithdrawRecordService;
import com.sqx.app.referral.withdraw.db.service.IWithdrawalAccountService;
import com.sqx.app.referral.withdraw.domain.bo.*;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawRecord;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawalAccount;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;
import com.sqx.app.referral.withdraw.service.WithdrawRecordService;
import com.sqx.app.referral.withdraw.enums.WithdrawSourceType;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.system.db.service.ISysConfigService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 提现记录 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-05 10:10:32
 */
@Service
public class WithdrawRecordServiceImpl implements WithdrawRecordService {

    @Resource
    private IWithdrawRecordService iWithdrawRecordService;
    @Resource
    private IReferralUserCommissionService iReferralUserCommissionService;
    @Resource
    private ISysConfigService iSysConfigService;
    @Resource
    private IUserService iUserService;
    @Resource
    private IWithdrawalAccountService iWithdrawalAccountService;
    @Resource
    private IReferralCommissionService iReferralCommissionService;
    @Resource
    private IUserWalletService iUserWalletService;
    @Resource
    private RentalWalletFlowService rentalWalletFlowService;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantWalletAccountService merchantWalletAccountService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveInfo(WithdrawRecordSaveBo withdrawRecordSaveBo) {
        int sourceType = withdrawRecordSaveBo.getSourceType() == null
                ? WithdrawSourceType.WALLET.getCode() : withdrawRecordSaveBo.getSourceType();
        if (WithdrawSourceType.WALLET.getCode() == sourceType) {
            saveWalletWithdraw(withdrawRecordSaveBo);
            return;
        }
        saveReferralWithdraw(withdrawRecordSaveBo);
    }

    private void saveReferralWithdraw(WithdrawRecordSaveBo withdrawRecordSaveBo) {
        // 用户收益余额
        ReferralUserCommission referralUserCommission = iReferralUserCommissionService.getByUserId(withdrawRecordSaveBo.getUserId());
        if (Objects.isNull(referralUserCommission)) {
            throw new BizException("用户收益不存在");
        }

        if (withdrawRecordSaveBo.getAmount().compareTo(referralUserCommission.getCommission()) > 0) {
            throw new BizException("余额不足");
        }

        String maxAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MAX);
        if (withdrawRecordSaveBo.getAmount().compareTo(new BigDecimal(maxAmount)) > 0) {
            throw new BizException("最大提现金额为" + maxAmount);
        }

        String minAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MIN);
        if (withdrawRecordSaveBo.getAmount().compareTo(new BigDecimal(minAmount)) < 0) {
            throw new BizException("最小提现金额为" + maxAmount);
        }

        // 计算到账金额
        String serviceFeeRate = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_RATE);
        BigDecimal serviceFee = withdrawRecordSaveBo.getAmount().multiply(new BigDecimal(serviceFeeRate));
        BigDecimal actualAmount = withdrawRecordSaveBo.getAmount().subtract(serviceFee);

        // 保存提现记录
        WithdrawRecord withdrawRecord = new WithdrawRecord();
        BeanUtils.copyProperties(withdrawRecordSaveBo, withdrawRecord);
        withdrawRecord.setSourceType(WithdrawSourceType.REFERRAL.getCode());
        withdrawRecord.setActualAmount(actualAmount);
        withdrawRecord.setServiceFee(serviceFee);
        withdrawRecord.setStatus(1);

        iWithdrawRecordService.save(withdrawRecord);

        // 扣除用户收益余额
        iReferralUserCommissionService.decr(withdrawRecordSaveBo.getUserId(), withdrawRecordSaveBo.getAmount());

        // 用户收益明细
        iReferralCommissionService.saveWithdraw(withdrawRecordSaveBo.getUserId(), withdrawRecordSaveBo.getAmount());
    }

    private void saveWalletWithdraw(WithdrawRecordSaveBo withdrawRecordSaveBo) {
        com.sqx.app.wallet.wallet.domain.entity.UserWallet wallet = iUserWalletService.getOne(
                new LambdaQueryWrapper<com.sqx.app.wallet.wallet.domain.entity.UserWallet>()
                        .eq(com.sqx.app.wallet.wallet.domain.entity.UserWallet::getUserId, withdrawRecordSaveBo.getUserId())
                        .last("LIMIT 1"));
        if (wallet == null || wallet.getAmount() == null
                || withdrawRecordSaveBo.getAmount().compareTo(wallet.getAmount()) > 0) {
            throw new BizException("钱包余额不足");
        }
        validateWithdrawAmount(withdrawRecordSaveBo.getAmount());

        String serviceFeeRate = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_RATE);
        BigDecimal serviceFee = withdrawRecordSaveBo.getAmount().multiply(new BigDecimal(serviceFeeRate));
        BigDecimal actualAmount = withdrawRecordSaveBo.getAmount().subtract(serviceFee);

        WithdrawRecord withdrawRecord = new WithdrawRecord();
        BeanUtils.copyProperties(withdrawRecordSaveBo, withdrawRecord);
        withdrawRecord.setSourceType(WithdrawSourceType.WALLET.getCode());
        withdrawRecord.setActualAmount(actualAmount);
        withdrawRecord.setServiceFee(serviceFee);
        withdrawRecord.setStatus(1);
        iWithdrawRecordService.save(withdrawRecord);

        iUserWalletService.decr(withdrawRecordSaveBo.getUserId(), withdrawRecordSaveBo.getAmount());
        rentalWalletFlowService.recordWithdraw(withdrawRecordSaveBo.getUserId(),
                withdrawRecord.getId(), withdrawRecordSaveBo.getAmount());
    }

    private void validateWithdrawAmount(BigDecimal amount) {
        String maxAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MAX);
        if (amount.compareTo(new BigDecimal(maxAmount)) > 0) {
            throw new BizException("最大提现金额为" + maxAmount);
        }
        String minAmount = iSysConfigService.getConfigValue(ReferralConstant.withdraw.SYS_WITHDRAW_MIN);
        if (amount.compareTo(new BigDecimal(minAmount)) < 0) {
            throw new BizException("最小提现金额为" + minAmount);
        }
    }

    @Override
    public IPage<WithdrawRecordVo> pageInfo(WithdrawRecordPageBo withdrawRecordPageBo) {
        Page<WithdrawRecord> initPage = PageUtil.initPage(withdrawRecordPageBo.getPage(), withdrawRecordPageBo.getLimit());

        QueryWrapper<WithdrawRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Objects.nonNull(withdrawRecordPageBo.getUserId()), WithdrawRecord::getUserId, withdrawRecordPageBo.getUserId())
                .eq(Objects.nonNull(withdrawRecordPageBo.getSourceType()), WithdrawRecord::getSourceType, withdrawRecordPageBo.getSourceType())
                .orderByDesc(WithdrawRecord::getCreateTime);

        IPage<WithdrawRecord> iPage = iWithdrawRecordService.page(initPage, queryWrapper);
        List<WithdrawRecordVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (WithdrawRecord record : iPage.getRecords()) {
                WithdrawRecordVo vo = new WithdrawRecordVo();
                BeanUtils.copyProperties(record, vo);

                UserEntity userEntity = iUserService.getById(record.getUserId());
                if (Objects.nonNull(userEntity)) {
                    vo.setUserEntity(userEntity);
                    vo.setPhone(userEntity.getPhone());
                    vo.setUserName(userEntity.getUserName());
                }

                WithdrawalAccount withdrawalAccount = iWithdrawalAccountService.getOne(new LambdaQueryWrapper<WithdrawalAccount>()
                        .eq(WithdrawalAccount::getUserId, record.getUserId())
                        .eq(WithdrawalAccount::getType, resolveWithdrawalAccountType(record.getAccountType()))
                );
                if (Objects.nonNull(withdrawalAccount)) {
                    vo.setWithdrawalAccount(withdrawalAccount);
                }

                voList.add(vo);
            }
        }

        IPage<WithdrawRecordVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public WithdrawRecordVo info(WithdrawRecordInfoBo withdrawRecordInfoBo) {
        WithdrawRecord withdrawRecord = iWithdrawRecordService.getById(withdrawRecordInfoBo.getId());
        if (Objects.isNull(withdrawRecord)) {
            throw new BizException("信息不存在");
        }

        WithdrawRecordVo vo = new WithdrawRecordVo();
        BeanUtils.copyProperties(withdrawRecord, vo);

        UserEntity userEntity = iUserService.getById(withdrawRecord.getUserId());
        if (Objects.nonNull(userEntity)) {
            vo.setUserEntity(userEntity);
            vo.setPhone(userEntity.getPhone());
            vo.setUserName(userEntity.getUserName());
        }

        return vo;
    }

    @Override
    public void updateInfo(WithdrawRecordUpdateBo withdrawRecordUpdateBo) {
        WithdrawRecord withdrawRecord = new WithdrawRecord();
        BeanUtils.copyProperties(withdrawRecordUpdateBo, withdrawRecord);

        iWithdrawRecordService.updateById(withdrawRecord);
    }

    @Override
    public void delete(WithdrawRecordInfoBo withdrawRecordInfoBo) {
        iWithdrawRecordService.removeById(withdrawRecordInfoBo.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void confirm(WithdrawRecordConfirmBo withdrawRecordConfirmBo) {
        WithdrawRecord withdrawRecord = iWithdrawRecordService.getById(withdrawRecordConfirmBo.getId());
        if (Objects.isNull(withdrawRecord)) {
            throw new BizException("提现信息不存在");
        }

        if (2 == withdrawRecordConfirmBo.getStatus()) {
            // 提现成功
            withdrawRecord.setStatus(2);
            withdrawRecord.setWithdrawTime(LocalDateTime.now());

            iWithdrawRecordService.updateById(withdrawRecord);
        } else if (3 == withdrawRecordConfirmBo.getStatus()) {
            // 拒绝提现
            withdrawRecord.setStatus(3);
            withdrawRecord.setReason(withdrawRecordConfirmBo.getReason());

            iWithdrawRecordService.updateById(withdrawRecord);

            if (Integer.valueOf(WithdrawSourceType.WALLET.getCode()).equals(withdrawRecord.getSourceType())) {
                iUserWalletService.incr(withdrawRecord.getUserId(), withdrawRecord.getAmount());
                rentalWalletFlowService.recordWithdrawRejectRefund(withdrawRecord.getUserId(),
                        withdrawRecord.getId(), withdrawRecord.getAmount());
            } else if (Integer.valueOf(WithdrawSourceType.MERCHANT_WALLET.getCode()).equals(withdrawRecord.getSourceType())) {
                Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getUserId, withdrawRecord.getUserId())
                        .last("LIMIT 1"));
                if (merchant != null) {
                    merchantWalletAccountService.recordTransaction(merchant.getId(), 11, 1, 11,
                            withdrawRecord.getId(), withdrawRecord.getAmount(), "商户提现驳回返还",
                            String.valueOf(withdrawRecord.getId()));
                    merchantWalletAccountService.refundWithdraw(merchant.getId(), withdrawRecord.getAmount());
                }
            } else {
                iReferralUserCommissionService.incr(withdrawRecord.getUserId(), withdrawRecord.getAmount());
                iReferralCommissionService.saveRefuseWithdraw(withdrawRecord.getUserId(), withdrawRecord.getAmount());
            }
        } else {
            throw new BizException("非法操作");
        }
    }

    /** 提现记录 accountType：1 微信 2 支付宝 → 提现账号 type：1 支付宝 2 微信 */
    private Integer resolveWithdrawalAccountType(Integer withdrawAccountType) {
        if (withdrawAccountType == null) {
            return null;
        }
        if (Integer.valueOf(1).equals(withdrawAccountType)) {
            return 2;
        }
        if (Integer.valueOf(2).equals(withdrawAccountType)) {
            return 1;
        }
        return withdrawAccountType;
    }
}
