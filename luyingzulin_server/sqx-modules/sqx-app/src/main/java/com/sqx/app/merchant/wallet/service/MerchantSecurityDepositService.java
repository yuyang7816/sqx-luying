package com.sqx.app.merchant.wallet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositAdjustBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositLogPageBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositPayBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundApplyBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundAuditBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundPageBo;
import com.sqx.app.merchant.wallet.domain.vo.AdminMerchantSecurityDepositRefundVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositLogVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositPayStatusVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantSecurityDepositSummaryVo;

import java.math.BigDecimal;

public interface MerchantSecurityDepositService {

    MerchantSecurityDepositSummaryVo summary(Long merchantId);

    IPage<MerchantSecurityDepositLogVo> logPage(Long merchantId, MerchantSecurityDepositLogPageBo pageBo);

    String applyPay(Long merchantId, MerchantSecurityDepositPayBo payBo);

    BigDecimal preparePay(Long merchantId, String payNo, BigDecimal amount);

    MerchantSecurityDepositPayStatusVo payStatus(Long merchantId, String payNo);

    void applyRefund(Long merchantId, MerchantSecurityDepositRefundApplyBo refundBo);

    void onPaySuccess(Long merchantId, String payNo, BigDecimal amount);

    BigDecimal getRequiredAmount();

    boolean isDepositRequired();

    boolean isDepositPaid(Long merchantId);

    void assertDepositPaidIfRequired(Long merchantId);

    void adminAdjust(MerchantSecurityDepositAdjustBo adjustBo);

    void cancelPendingPay(Long merchantId, String payNo);

    IPage<AdminMerchantSecurityDepositRefundVo> adminRefundPage(MerchantSecurityDepositRefundPageBo pageBo);

    void adminAuditRefund(MerchantSecurityDepositRefundAuditBo auditBo);
}
