package com.sqx.app.merchant.constants;

/**
 * 商户相关系统配置键
 */
public interface MerchantConstant {

    interface securityDeposit {
        /**
         * 商户应缴保证金金额（元），0 表示无需缴纳
         */
        String MERCHANT_SECURITY_DEPOSIT_AMOUNT = "merchant_security_deposit_amount";
    }
}
