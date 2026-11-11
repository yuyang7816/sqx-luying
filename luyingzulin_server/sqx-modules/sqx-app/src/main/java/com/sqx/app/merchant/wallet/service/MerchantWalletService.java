package com.sqx.app.merchant.wallet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositLogPageBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositPayBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundApplyBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantWalletTransactionPageBo;
import com.sqx.app.merchant.wallet.domain.vo.*;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordPageBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;

public interface MerchantWalletService {

    MerchantWalletSummaryVo summary(Long operatorUserId);

    IPage<MerchantWalletTransactionVo> transactionPage(Long operatorUserId, MerchantWalletTransactionPageBo pageBo);

    void withdraw(Long operatorUserId, WithdrawRecordSaveBo saveBo);

    IPage<WithdrawRecordVo> withdrawPage(Long operatorUserId, WithdrawRecordPageBo pageBo);

    MerchantSecurityDepositSummaryVo securityDepositSummary(Long operatorUserId);

    IPage<MerchantSecurityDepositLogVo> securityDepositLogPage(Long operatorUserId, MerchantSecurityDepositLogPageBo pageBo);

    String securityDepositPay(Long operatorUserId, MerchantSecurityDepositPayBo payBo);

    MerchantSecurityDepositPayStatusVo securityDepositPayStatus(Long operatorUserId, String payNo);

    void securityDepositPayCancel(Long operatorUserId, String payNo);

    void securityDepositRefundApply(Long operatorUserId, MerchantSecurityDepositRefundApplyBo refundBo);
}
