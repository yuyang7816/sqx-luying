package com.sqx.app.rental.order.support;

import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M04：订单状态机生命周期单元测试
 */
class RentOrderStateMachineM04Test {

    @Test
    void assertCanPickupShouldAllowWaitingPickup() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanPickup(OrderStatus.WAITING_PICKUP.getCode()));
    }

    @Test
    void assertCanPickupShouldRejectRenting() {
        assertThrows(BizException.class,
                () -> RentOrderStateMachine.assertCanPickup(OrderStatus.RENTING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowWaitingPickupToRenting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.WAITING_PICKUP.getCode(), OrderStatus.RENTING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowRentingToReturning() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.RENTING.getCode(), OrderStatus.RETURNING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowReturningToCompleted() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.RETURNING.getCode(), OrderStatus.COMPLETED.getCode()));
    }

    @Test
    void assertTransitionShouldRejectPendingPaymentToRenting() {
        assertThrows(BizException.class, () -> RentOrderStateMachine.assertTransition(
                OrderStatus.PENDING_PAYMENT.getCode(), OrderStatus.RENTING.getCode()));
    }

    @Test
    void assertCanRenewShouldOnlyAllowRenting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanRenew(OrderStatus.RENTING.getCode()));
        assertThrows(BizException.class,
                () -> RentOrderStateMachine.assertCanRenew(OrderStatus.WAITING_PICKUP.getCode()));
    }

    @Test
    void assertCanPickupShouldAllowDelivering() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanPickup(OrderStatus.DELIVERING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowDeliveringToRenting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.DELIVERING.getCode(), OrderStatus.RENTING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowReturningToInspecting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.RETURNING.getCode(), OrderStatus.INSPECTING.getCode()));
    }

    @Test
    void assertTransitionShouldAllowInspectingToCompleted() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertTransition(
                OrderStatus.INSPECTING.getCode(), OrderStatus.COMPLETED.getCode()));
    }

    @Test
    void assertCanReturnApplyShouldOnlyAllowRenting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanReturnApply(OrderStatus.RENTING.getCode()));
        assertThrows(BizException.class,
                () -> RentOrderStateMachine.assertCanReturnApply(OrderStatus.RETURNING.getCode()));
    }

    @Test
    void assertCanInspectShouldAllowReturningAndInspecting() {
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanInspect(OrderStatus.RETURNING.getCode()));
        assertDoesNotThrow(() -> RentOrderStateMachine.assertCanInspect(OrderStatus.INSPECTING.getCode()));
        assertThrows(BizException.class,
                () -> RentOrderStateMachine.assertCanInspect(OrderStatus.RENTING.getCode()));
    }

    @Test
    void assertTransitionShouldRejectCompletedToRenting() {
        assertThrows(BizException.class, () -> RentOrderStateMachine.assertTransition(
                OrderStatus.COMPLETED.getCode(), OrderStatus.RENTING.getCode()));
    }

    @Test
    void returningTabStatusesShouldIncludeReturningAndInspecting() {
        assertTrue(RentOrderStateMachine.returningTabStatuses().contains(OrderStatus.RETURNING.getCode()));
        assertTrue(RentOrderStateMachine.returningTabStatuses().contains(OrderStatus.INSPECTING.getCode()));
    }
}
