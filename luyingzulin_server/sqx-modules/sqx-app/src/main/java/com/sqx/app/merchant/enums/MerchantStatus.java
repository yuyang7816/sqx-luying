package com.sqx.app.merchant.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 商户主表状态
 */
public enum MerchantStatus {

    ACTIVE("active", "启用"),
    FROZEN("frozen", "冻结");

    private final String code;
    private final String desc;

    MerchantStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MerchantStatus fromCode(String code) {
        for (MerchantStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        throw new BizException("不支持的商户状态");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
