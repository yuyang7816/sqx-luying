package com.sqx.app.merchant.wallet.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositLogPageBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositPayBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundApplyBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantWalletTransactionPageBo;
import com.sqx.app.merchant.wallet.domain.vo.*;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.app.merchant.wallet.service.MerchantWalletAccountService;
import com.sqx.app.merchant.wallet.service.MerchantWalletService;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordPageBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;
import com.sqx.app.referral.withdraw.enums.WithdrawSourceType;
import com.sqx.app.referral.withdraw.service.WithdrawRecordService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class MerchantWalletServiceImpl implements MerchantWalletService {

    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private MerchantWalletAccountService merchantWalletAccountService;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;
    @Resource
    private WithdrawRecordService withdrawRecordService;

    @Override
    public MerchantWalletSummaryVo summary(Long operatorUserId) {
        MerchantContextSupport.MerchantContext context = merchantContextSupport.requireMerchantContext(operatorUserId);
        MerchantWalletSummaryVo vo = new MerchantWalletSummaryVo();
        vo.setWallet(merchantWalletAccountService.summary(context.getMerchantId()));
        vo.setSecurityDeposit(merchantSecurityDepositService.summary(context.getMerchantId()));

        MerchantWalletTransactionPageBo txBo = new MerchantWalletTransactionPageBo();
        txBo.setPage(1L);
        txBo.setLimit(8L);
        vo.setRecentTransactions(merchantWalletAccountService.transactionPage(context.getMerchantId(), txBo).getRecords());

        WithdrawRecordPageBo withdrawBo = new WithdrawRecordPageBo();
        withdrawBo.setPage(1L);
        withdrawBo.setLimit(5L);
        withdrawBo.setUserId(context.getOwnerUserId());
        withdrawBo.setSourceType(WithdrawSourceType.MERCHANT_WALLET.getCode());
        vo.setRecentWithdraws(withdrawRecordService.pageInfo(withdrawBo).getRecords());
        return vo;
    }

    @Override
    public IPage<MerchantWalletTransactionVo> transactionPage(Long operatorUserId, MerchantWalletTransactionPageBo pageBo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        return merchantWalletAccountService.transactionPage(merchantId, pageBo);
    }

    @Override
    public void withdraw(Long operatorUserId, WithdrawRecordSaveBo saveBo) {
        MerchantContextSupport.MerchantContext context = merchantContextSupport.requireMerchantContextForOperation(operatorUserId);
        saveBo.setUserId(context.getOwnerUserId());
        saveBo.setSourceType(WithdrawSourceType.MERCHANT_WALLET.getCode());
        merchantWalletAccountService.withdraw(context.getMerchantId(), context.getOwnerUserId(), saveBo);
    }

    @Override
    public IPage<WithdrawRecordVo> withdrawPage(Long operatorUserId, WithdrawRecordPageBo pageBo) {
        MerchantContextSupport.MerchantContext context = merchantContextSupport.requireMerchantContext(operatorUserId);
        pageBo.setUserId(context.getOwnerUserId());
        pageBo.setSourceType(WithdrawSourceType.MERCHANT_WALLET.getCode());
        return withdrawRecordService.pageInfo(pageBo);
    }

    @Override
    public MerchantSecurityDepositSummaryVo securityDepositSummary(Long operatorUserId) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        return merchantSecurityDepositService.summary(merchantId);
    }

    @Override
    public IPage<MerchantSecurityDepositLogVo> securityDepositLogPage(Long operatorUserId, MerchantSecurityDepositLogPageBo pageBo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        return merchantSecurityDepositService.logPage(merchantId, pageBo);
    }

    @Override
    public String securityDepositPay(Long operatorUserId, MerchantSecurityDepositPayBo payBo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        return merchantSecurityDepositService.applyPay(merchantId, payBo);
    }

    @Override
    public MerchantSecurityDepositPayStatusVo securityDepositPayStatus(Long operatorUserId, String payNo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        return merchantSecurityDepositService.payStatus(merchantId, payNo);
    }

    @Override
    public void securityDepositPayCancel(Long operatorUserId, String payNo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        merchantSecurityDepositService.cancelPendingPay(merchantId, payNo);
    }

    @Override
    public void securityDepositRefundApply(Long operatorUserId, MerchantSecurityDepositRefundApplyBo refundBo) {
        Long merchantId = merchantContextSupport.requireMerchantContext(operatorUserId).getMerchantId();
        merchantSecurityDepositService.applyRefund(merchantId, refundBo);
    }
}
