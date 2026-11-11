package com.sqx.app.rental.user.enums;

import com.sqx.common.core.exception.BizException;
import lombok.Getter;

/**
 * 用户优惠券状态
 */
@Getter
public enum UserCouponStatus {
    UNUSED("unused", "未使用"),
    USED("used", "已使用"),
    EXPIRED("expired", "已过期");

    private final String code;
    private final String label;

    UserCouponStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static UserCouponStatus fromCode(String code) {
        for (UserCouponStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new BizException("优惠券状态无效");
    }
}
