package com.sqx.app.merchant.enums;

import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.common.core.exception.BizException;

import java.util.Objects;

/**
 * 商户订单状态（B 端视角）
 */
public enum MerchantOrderStatus {

    PENDING_PREPARE("pending_prepare", "待备货", "pending"),
    PREPARED("prepared", "待取货", "pending"),
    RENTING("renting", "租赁中", "running"),
    RETURNING("returning", "待验收", "review"),
    INSPECTING("inspecting", "验收中", "review"),
    COMPLETED("completed", "已完成", "completed");

    private final String code;
    private final String desc;
    private final String tabKey;

    MerchantOrderStatus(String code, String desc, String tabKey) {
        this.code = code;
        this.desc = desc;
        this.tabKey = tabKey;
    }

    public static MerchantOrderStatus fromCode(String code) {
        for (MerchantOrderStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        throw new BizException("不支持的商户订单状态");
    }

    /** 用户订单状态 → 商户订单状态 */
    public static String fromRentOrderStatus(String rentStatus) {
        if (OrderStatus.WAITING_PICKUP.getCode().equals(rentStatus)
                || OrderStatus.DELIVERING.getCode().equals(rentStatus)) {
            return PENDING_PREPARE.getCode();
        }
        if (OrderStatus.RENTING.getCode().equals(rentStatus)) {
            return RENTING.getCode();
        }
        if (OrderStatus.RETURNING.getCode().equals(rentStatus)) {
            return RETURNING.getCode();
        }
        if (OrderStatus.INSPECTING.getCode().equals(rentStatus)) {
            return INSPECTING.getCode();
        }
        if (OrderStatus.COMPLETED.getCode().equals(rentStatus)) {
            return COMPLETED.getCode();
        }
        return PENDING_PREPARE.getCode();
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getTabKey() {
        return tabKey;
    }
}
