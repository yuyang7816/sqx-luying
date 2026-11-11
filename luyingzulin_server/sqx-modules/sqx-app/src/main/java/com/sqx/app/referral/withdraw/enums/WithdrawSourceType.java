package com.sqx.app.referral.withdraw.enums;

import lombok.Getter;

/**
 * 提现来源：1佣金 2钱包
 */
@Getter
public enum WithdrawSourceType {
    REFERRAL(1, "佣金提现"),
    WALLET(2, "用户钱包提现"),
    MERCHANT_WALLET(3, "商户钱包提现");

    private final int code;
    private final String label;

    WithdrawSourceType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
