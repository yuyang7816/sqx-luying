package com.sqx.app.rental.user.enums;

import com.sqx.common.core.exception.BizException;
import lombok.Getter;

/**
 * 优惠券模板类型
 */
@Getter
public enum CouponType {
    AMOUNT_OFF("amount_off", "满减券"),
    DISCOUNT("discount", "折扣券"),
    DELIVERY_FREE("delivery_free", "免配送券"),
    DEPOSIT_FREE("deposit_free", "免押券");

    private final String code;
    private final String label;

    CouponType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static CouponType fromCode(String code) {
        for (CouponType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new BizException("优惠券类型无效");
    }
}
