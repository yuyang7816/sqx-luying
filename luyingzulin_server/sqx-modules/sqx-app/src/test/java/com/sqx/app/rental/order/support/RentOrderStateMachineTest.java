package com.sqx.app.rental.order.support;

import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M03：订单状态机单元测试
 */
class RentOrderStateMachineTest {

    @Test
    void resolvePaidStatusShouldUseDeliveringForDelivery() {
        assertEquals(OrderStatus.DELIVERING.getCode(),
                RentOrderStateMachine.resolvePaidStatus(PickupType.DELIVERY.getCode()));
    }

    @Test
    void resolvePaidStatusShouldUseWaitingPickupForStore() {
        assertEquals(OrderStatus.WAITING_PICKUP.getCode(),
                RentOrderStateMachine.resolvePaidStatus(PickupType.STORE.getCode()));
    }

    @Test
    void assertCanCancelShouldRejectPaidOrder() {
        assertThrows(BizException.class,
                () -> RentOrderStateMachine.assertCanCancel(OrderStatus.WAITING_PICKUP.getCode()));
    }

    @Test
    void isPaidStatusShouldRecognizeWaitingPickup() {
        assertTrue(RentOrderStateMachine.isPaidStatus(OrderStatus.WAITING_PICKUP.getCode()));
    }
}
