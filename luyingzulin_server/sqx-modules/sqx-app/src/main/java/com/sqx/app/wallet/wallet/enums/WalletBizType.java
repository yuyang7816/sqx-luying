package com.sqx.app.wallet.wallet.enums;

import lombok.Getter;

/**
 * 钱包业务类型（扩展租赁场景）
 */
@Getter
public enum WalletBizType {
    VIP(1, "会员购买"),
    ORDER(2, "订单支付"),
    ACTIVITY(3, "活动奖励"),
    REFUND(5, "退款"),
    DEPOSIT_REFUND_LEGACY(6, "退保证金"),
    RENT_PAY(7, "租赁支付"),
    DEPOSIT_FREEZE(8, "押金冻结"),
    DEPOSIT_REFUND(9, "押金退还"),
    WITHDRAW(10, "余额提现"),
    WITHDRAW_REJECT(11, "提现驳回返还"),
    RECHARGE(12, "余额充值");

    private final int code;
    private final String label;

    WalletBizType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
