package com.sqx.app.rental.enums;

import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 租赁订单状态
 */
public enum OrderStatus {

    PENDING_PAYMENT("pending_payment", "待支付"),
    WAITING_PICKUP("waiting_pickup", "待取货"),
    DELIVERING("delivering", "配送中"),
    RENTING("renting", "租赁中"),
    RETURNING("returning", "归还中"),
    INSPECTING("inspecting", "验收中"),
    COMPLETED("completed", "已完成"),
    CANCELLED("cancelled", "已取消"),
    REFUNDING("refunding", "退款中"),
    ;

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (Objects.equals(status.getCode(), code)) {
                return status;
            }
        }
        throw new BizException("不支持的订单状态");
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
