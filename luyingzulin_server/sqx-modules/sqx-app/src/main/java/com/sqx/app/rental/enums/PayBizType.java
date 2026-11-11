package com.sqx.app.rental.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 租赁支付业务子类型（订单支付记录用）
 */
public enum PayBizType {

    RENT_ORDER("rent_order", "租赁下单"),
    RENT_RENEW("rent_renew", "续租"),
    DEPOSIT_PAY("deposit_pay", "押金缴纳"),
    RENT_FEE("rent_fee", "租金"),
    DEPOSIT("deposit", "押金"),
    DELIVERY("delivery", "配送费"),
    ;

    private final String code;
    private final String desc;

    PayBizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayBizType fromCode(String code) {
        for (PayBizType payBizType : values()) {
            if (Objects.equals(payBizType.getCode(), code)) {
                return payBizType;
            }
        }
        throw new BizException("不支持的支付业务类型");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
