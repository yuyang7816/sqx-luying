package com.sqx.app.merchant.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 商户入驻申请状态
 */
public enum ApplyStatus {

    PENDING("pending", "待提交"),
    REVIEWING("reviewing", "审核中"),
    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已驳回");

    private final String code;
    private final String desc;

    ApplyStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ApplyStatus fromCode(String code) {
        for (ApplyStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        throw new BizException("不支持的入驻申请状态");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
