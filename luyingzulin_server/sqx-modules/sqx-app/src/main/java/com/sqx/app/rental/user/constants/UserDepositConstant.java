package com.sqx.app.rental.user.constants;

/**
 * 用户押金相关配置键
 */
public interface UserDepositConstant {

    interface config {
        /** 账户需保留的最低可用押金余额（元，0 表示不限制） */
        String USER_MIN_DEPOSIT_BALANCE = "user_min_deposit_balance";
    }
}
