package com.sqx.app.merchant.wallet.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.wallet.domain.bo.MerchantWalletTransactionPageBo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantWalletTransactionVo;
import com.sqx.app.merchant.wallet.domain.vo.MerchantWalletVo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;

public interface MerchantWalletAccountService {

    MerchantWalletVo summary(Long merchantId);

    IPage<MerchantWalletTransactionVo> transactionPage(Long merchantId, MerchantWalletTransactionPageBo pageBo);

    void withdraw(Long merchantId, Long ownerUserId, WithdrawRecordSaveBo saveBo);

    void recordTransaction(Long merchantId, Integer transactionType, Integer direction, Integer bizType,
                           Long bizId, java.math.BigDecimal amount, String title, String description);

    void refundWithdraw(Long merchantId, java.math.BigDecimal amount);
}
