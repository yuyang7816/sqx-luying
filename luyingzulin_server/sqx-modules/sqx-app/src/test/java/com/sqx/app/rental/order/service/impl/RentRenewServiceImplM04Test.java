package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.domain.entity.RentRenewRecord;
import com.sqx.app.rental.order.domain.vo.RentRenewPreviewVo;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentOrderPaymentMapper;
import com.sqx.app.rental.order.mapper.RentRenewRecordMapper;
import com.sqx.app.rental.stock.service.RentStockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M04：续租租期延长与支付成功单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentRenewServiceImplM04Test {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, RentOrder.class);
        TableInfoHelper.initTableInfo(assistant, RentOrderItem.class);
        TableInfoHelper.initTableInfo(assistant, RentRenewRecord.class);
    }

    @Mock
    private RentOrderMapper rentOrderMapper;
    @Mock
    private RentOrderItemMapper rentOrderItemMapper;
    @Mock
    private RentRenewRecordMapper rentRenewRecordMapper;
    @Mock
    private RentOrderPaymentMapper rentOrderPaymentMapper;
    @Mock
    private RentOrderLogMapper rentOrderLogMapper;
    @Mock
    private RentGoodsMapper rentGoodsMapper;
    @Mock
    private RentStockService rentStockService;

    @InjectMocks
    private RentRenewServiceImpl rentRenewService;

    @Test
    void previewShouldCalculateHourlyRenewAmount() {
        LocalDateTime endTime = LocalDateTime.of(2026, 9, 3, 18, 0);
        LocalDateTime newEndTime = endTime.plusDays(1);
        RentOrder order = rentingOrder(endTime);
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        RentOrderItem item = orderItem(order.getId());
        when(rentOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(item));
        when(rentGoodsMapper.selectById(1L)).thenReturn(onShelfGoods());

        RentRenewPreviewVo vo = rentRenewService.preview(1L, 10L, "2026-09-04 18:00:00");

        assertEquals(newEndTime, vo.getNewEndTime());
        assertEquals(24, vo.getExtraHours());
        assertEquals(new BigDecimal("10.00"), vo.getHourUnitPrice());
        assertEquals(new BigDecimal("240.00"), vo.getRenewAmount());
    }

    @Test
    void handleRenewPaySuccessShouldExtendOrderEndTimeAndRentAmount() {
        LocalDateTime oldEnd = LocalDateTime.of(2026, 9, 3, 18, 0);
        LocalDateTime newEnd = oldEnd.plusDays(3);
        RentOrder order = rentingOrder(oldEnd);
        order.setRentAmount(new BigDecimal("100.00"));
        order.setStartTime(LocalDateTime.of(2026, 9, 1, 10, 0));

        RentRenewRecord record = new RentRenewRecord();
        record.setId(1L);
        record.setRenewNo("RN20260901001");
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(1L);
        record.setExtraDays(72);
        record.setOldEndTime(oldEnd);
        record.setNewEndTime(newEnd);
        record.setRenewAmount(new BigDecimal("150.00"));
        record.setPayStatus("pending");

        when(rentRenewRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);
        when(rentOrderMapper.selectById(order.getId())).thenReturn(order);
        when(rentOrderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RentOrderItem item = orderItem(order.getId());
        item.setStartTime(order.getStartTime());
        item.setEndTime(oldEnd);
        when(rentOrderItemMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(item));
        when(rentOrderItemMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rentRenewRecordMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        rentRenewService.handleRenewPaySuccess("RN20260901001", "wx", "TX001", new BigDecimal("150.00"));

        verify(rentStockService).extendOccupyEndTime(eq(order.getId()), eq(newEnd));

        ArgumentCaptor<LambdaUpdateWrapper<RentOrder>> orderCaptor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(rentOrderMapper).update(any(), orderCaptor.capture());

        verify(rentOrderPaymentMapper).insert(any());
        verify(rentOrderLogMapper).insert(any());
    }

    @Test
    void handleRenewPaySuccessShouldBeIdempotentWhenAlreadyPaid() {
        RentRenewRecord record = new RentRenewRecord();
        record.setRenewNo("RN20260901001");
        record.setPayStatus("success");
        when(rentRenewRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(record);

        rentRenewService.handleRenewPaySuccess("RN20260901001", "wx", "TX001", new BigDecimal("100.00"));

        verify(rentOrderMapper, org.mockito.Mockito.never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    private static RentOrder rentingOrder(LocalDateTime endTime) {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.RENTING.getCode());
        order.setRentType("day");
        order.setEndTime(endTime);
        return order;
    }

    private static RentOrderItem orderItem(Long orderId) {
        RentOrderItem item = new RentOrderItem();
        item.setId(1L);
        item.setOrderId(orderId);
        item.setGoodsId(1L);
        item.setQty(1);
        return item;
    }

    private static RentGoods onShelfGoods() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setRentPriceDay(new BigDecimal("50.00"));
        goods.setRentPriceHour(new BigDecimal("10.00"));
        return goods;
    }
}
