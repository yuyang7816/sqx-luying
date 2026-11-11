package com.sqx.app.rental.user.enums;

import com.sqx.common.core.exception.BizException;
import lombok.Getter;

/**
 * 押金流水类型
 */
@Getter
public enum DepositLogType {
    PAY("pay", "缴纳租赁押金"),
    FREEZE("freeze", "冻结租赁押金"),
    UNFREEZE("unfreeze", "解冻租赁押金"),
    REFUND("refund", "退还租赁押金"),
    DEDUCT("deduct", "扣款"),
    ADJUST("adjust", "运营调整");

    private final String code;
    private final String label;

    DepositLogType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static DepositLogType fromCode(String code) {
        for (DepositLogType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BizException("押金流水类型无效");
    }
}
