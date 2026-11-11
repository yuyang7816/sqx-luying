package com.sqx.app.rental.order.service.impl;

import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.vo.RentOrderStatusCountVo;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M01：订单角标统计单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentOrderServiceImplTest {

    @Mock
    private RentOrderMapper rentOrderMapper;

    @InjectMocks
    private RentOrderServiceImpl rentOrderService;

    @Test
    void statusCountsShouldAggregateByMyPageTabs() {
        when(rentOrderMapper.selectList(any())).thenReturn(Arrays.asList(
                order("pending_payment"),
                order("waiting_pickup"),
                order("delivering"),
                order("renting"),
                order("returning"),
                order("inspecting"),
                order("refunding"),
                order("completed"),
                order("cancelled")
        ));

        assertEquals(1, rentOrderService.statusCounts(1L).getPendingPayment());
        assertEquals(2, rentOrderService.statusCounts(1L).getWaitingPickup());
        assertEquals(1, rentOrderService.statusCounts(1L).getRenting());
        assertEquals(2, rentOrderService.statusCounts(1L).getReturning());
        assertEquals(1, rentOrderService.statusCounts(1L).getAfterSale());
    }

    @Test
    void statusCountsShouldReturnZeroWhenNoOrders() {
        when(rentOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        RentOrderStatusCountVo counts = rentOrderService.statusCounts(99L);
        assertEquals(0, counts.getPendingPayment());
        assertEquals(0, counts.getWaitingPickup());
        assertEquals(0, counts.getRenting());
        assertEquals(0, counts.getReturning());
        assertEquals(0, counts.getAfterSale());
    }

    @Test
    void statusCountsShouldIgnoreCompletedAndCancelled() {
        when(rentOrderMapper.selectList(any())).thenReturn(Arrays.asList(
                order("completed"),
                order("cancelled"),
                order("renting")
        ));

        RentOrderStatusCountVo counts = rentOrderService.statusCounts(2L);
        assertEquals(0, counts.getPendingPayment());
        assertEquals(1, counts.getRenting());
    }

    private static RentOrder order(String status) {
        RentOrder order = new RentOrder();
        order.setStatus(status);
        return order;
    }
}
