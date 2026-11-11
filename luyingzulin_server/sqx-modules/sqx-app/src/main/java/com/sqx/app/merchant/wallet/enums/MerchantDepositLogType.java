package com.sqx.app.merchant.wallet.enums;

import com.sqx.common.core.exception.BizException;
import lombok.Getter;

@Getter
public enum MerchantDepositLogType {
    PAY("pay", "缴纳保证金"),
    REFUND("refund", "退还保证金"),
    ADJUST("adjust", "平台调整");

    private final String code;
    private final String label;

    MerchantDepositLogType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static MerchantDepositLogType fromCode(String code) {
        for (MerchantDepositLogType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BizException("保证金流水类型无效");
    }
}
