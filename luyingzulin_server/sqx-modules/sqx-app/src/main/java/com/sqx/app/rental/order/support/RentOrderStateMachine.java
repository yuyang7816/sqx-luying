package com.sqx.app.rental.order.support;

import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.common.core.exception.BizException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 租赁订单状态机（M03 下单支付 + M04 取货/续租/归还/验收）
 */
public final class RentOrderStateMachine {

    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT.getCode(),
                set(OrderStatus.CANCELLED.getCode()));
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT.getCode() + ":pay_store",
                set(OrderStatus.WAITING_PICKUP.getCode()));
        TRANSITIONS.put(OrderStatus.PENDING_PAYMENT.getCode() + ":pay_delivery",
                set(OrderStatus.DELIVERING.getCode()));

        TRANSITIONS.put(OrderStatus.WAITING_PICKUP.getCode(),
                set(OrderStatus.RENTING.getCode()));
        TRANSITIONS.put(OrderStatus.DELIVERING.getCode(),
                set(OrderStatus.RENTING.getCode()));

        TRANSITIONS.put(OrderStatus.RENTING.getCode(),
                set(OrderStatus.RENTING.getCode(), OrderStatus.RETURNING.getCode()));

        TRANSITIONS.put(OrderStatus.RETURNING.getCode(),
                set(OrderStatus.INSPECTING.getCode(), OrderStatus.COMPLETED.getCode()));

        TRANSITIONS.put(OrderStatus.INSPECTING.getCode(),
                set(OrderStatus.COMPLETED.getCode()));
    }

    private RentOrderStateMachine() {
    }

    private static Set<String> set(String... codes) {
        return new HashSet<>(Arrays.asList(codes));
    }

    public static void assertTransition(String fromStatus, String toStatus) {
        if (fromStatus == null || toStatus == null) {
            throw new BizException("订单状态无效");
        }
        if (fromStatus.equals(toStatus)) {
            return;
        }
        Set<String> allowed = TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            throw new BizException("订单状态不允许从「" + OrderStatus.fromCode(fromStatus).getDesc()
                    + "」变更为「" + OrderStatus.fromCode(toStatus).getDesc() + "」");
        }
    }

    public static void assertCanCancel(String currentStatus) {
        if (OrderStatus.PENDING_PAYMENT.getCode().equals(currentStatus)
                || OrderStatus.WAITING_PICKUP.getCode().equals(currentStatus)
                || OrderStatus.DELIVERING.getCode().equals(currentStatus)) {
            return;
        }
        throw new BizException("当前订单状态不可取消");
    }

    public static boolean requiresRefundOnCancel(String currentStatus) {
        return OrderStatus.WAITING_PICKUP.getCode().equals(currentStatus)
                || OrderStatus.DELIVERING.getCode().equals(currentStatus);
    }

    public static void assertCanPickup(String currentStatus) {
        if (!OrderStatus.WAITING_PICKUP.getCode().equals(currentStatus)
                && !OrderStatus.DELIVERING.getCode().equals(currentStatus)) {
            throw new BizException("当前订单状态不可确认取货");
        }
    }

    public static void assertCanRenew(String currentStatus) {
        if (!OrderStatus.RENTING.getCode().equals(currentStatus)) {
            throw new BizException("仅租赁中订单可续租");
        }
    }

    public static void assertCanReturnApply(String currentStatus) {
        if (!OrderStatus.RENTING.getCode().equals(currentStatus)) {
            throw new BizException("仅租赁中订单可提交归还");
        }
    }

    public static void assertCanInspect(String currentStatus) {
        if (!OrderStatus.RETURNING.getCode().equals(currentStatus)
                && !OrderStatus.INSPECTING.getCode().equals(currentStatus)) {
            throw new BizException("当前订单状态不可验收");
        }
    }

    public static String resolvePaidStatus(String pickupType) {
        PickupType type = PickupType.fromCode(pickupType);
        if (PickupType.DELIVERY == type) {
            return OrderStatus.DELIVERING.getCode();
        }
        return OrderStatus.WAITING_PICKUP.getCode();
    }

    public static void assertCanPay(String currentStatus) {
        if (!OrderStatus.PENDING_PAYMENT.getCode().equals(currentStatus)) {
            throw new BizException("订单状态不可支付");
        }
    }

    public static boolean isPaidStatus(String status) {
        return OrderStatus.WAITING_PICKUP.getCode().equals(status)
                || OrderStatus.DELIVERING.getCode().equals(status)
                || OrderStatus.RENTING.getCode().equals(status)
                || OrderStatus.RETURNING.getCode().equals(status)
                || OrderStatus.INSPECTING.getCode().equals(status)
                || OrderStatus.COMPLETED.getCode().equals(status);
    }

    /** 用户端 Tab「待取货」包含的状态 */
    public static Set<String> waitingPickupTabStatuses() {
        return Collections.unmodifiableSet(set(
                OrderStatus.WAITING_PICKUP.getCode(),
                OrderStatus.DELIVERING.getCode()));
    }

    /** 用户端 Tab「待归还」包含的状态 */
    public static Set<String> returningTabStatuses() {
        return Collections.unmodifiableSet(set(
                OrderStatus.RETURNING.getCode(),
                OrderStatus.INSPECTING.getCode()));
    }
}
