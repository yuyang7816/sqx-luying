package com.sqx.app.rental.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 租赁类型
 */
public enum RentType {

    DAY("day", "日租"),
    HOUR("hour", "时租"),
    OVERNIGHT("overnight", "过夜租"),
    ;

    private final String code;
    private final String desc;

    RentType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RentType fromCode(String code) {
        for (RentType rentType : values()) {
            if (Objects.equals(rentType.getCode(), code)) {
                return rentType;
            }
        }
        throw new BizException("不支持的租赁类型");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
