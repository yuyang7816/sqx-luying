package com.sqx.app.rental.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 取货方式
 */
public enum PickupType {

    STORE("store", "门店自提"),
    PICKUP_POINT("pickup_point", "自提点"),
    DELIVERY("delivery", "同城配送"),
    ;

    private final String code;
    private final String desc;

    PickupType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PickupType fromCode(String code) {
        for (PickupType pickupType : values()) {
            if (Objects.equals(pickupType.getCode(), code)) {
                return pickupType;
            }
        }
        throw new BizException("不支持的取货方式");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
