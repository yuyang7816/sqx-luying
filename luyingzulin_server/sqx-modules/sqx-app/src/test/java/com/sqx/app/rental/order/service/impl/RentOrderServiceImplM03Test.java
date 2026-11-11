package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.order.support.RentOrderRefundSupport;
import com.sqx.app.rental.order.support.RentalDeliveryFeeSupport;
import com.sqx.app.rental.cart.service.RentCartService;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsPackageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsSpecMapper;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateBo;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateItemBo;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderPayment;
import com.sqx.app.rental.order.domain.entity.RentPickupCode;
import com.sqx.app.rental.order.domain.vo.RentOrderCreateVo;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentOrderPaymentMapper;
import com.sqx.app.rental.order.mapper.RentPickupCodeMapper;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.app.rental.user.service.UserCouponService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.common.core.exception.BizException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * M03：订单创建/取消/支付单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentOrderServiceImplM03Test {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                RentOrder.class);
    }

    @Mock
    private RentOrderMapper rentOrderMapper;
    @Mock
    private RentOrderItemMapper rentOrderItemMapper;
    @Mock
    private RentOrderLogMapper rentOrderLogMapper;
    @Mock
    private RentOrderPaymentMapper rentOrderPaymentMapper;
    @Mock
    private RentPickupCodeMapper rentPickupCodeMapper;
    @Mock
    private RentGoodsMapper rentGoodsMapper;
    @Mock
    private RentGoodsSpecMapper rentGoodsSpecMapper;
    @Mock
    private RentGoodsPackageMapper rentGoodsPackageMapper;
    @Mock
    private RentStockService rentStockService;
    @Mock
    private RentCartService rentCartService;
    @Mock
    private UserCouponService userCouponService;
    @Mock
    private RentalDeliveryFeeSupport rentalDeliveryFeeSupport;
    @Mock
    private UserDepositService userDepositService;
    @Mock
    private RentalWalletFlowService rentalWalletFlowService;
    @Mock
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Mock
    private RentOrderRefundSupport rentOrderRefundSupport;
    @Mock
    private MerchantStoreOpenSupport merchantStoreOpenSupport;
    @Mock
    private com.sqx.app.rental.order.support.RentOrderNotificationSupport rentOrderNotificationSupport;

    @InjectMocks
    private RentOrderServiceImpl rentOrderService;

    private void stubCreateOrderPricing(BigDecimal deliveryAmount) {
        when(rentalDeliveryFeeSupport.calc(anyString(), any(), any())).thenReturn(deliveryAmount);
        when(userCouponService.previewDiscount(anyLong(), any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void createOrderShouldReturnPendingPayment() {
        RentGoods goods = onShelfGoods();
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);
        stubCreateOrderPricing(BigDecimal.ZERO);
        doAnswer(invocation -> {
            RentOrder order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        }).when(rentOrderMapper).insert(any(RentOrder.class));

        RentOrderCreateItemBo item = new RentOrderCreateItemBo();
        item.setGoodsId(1L);
        item.setRentType("day");
        item.setStartTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        item.setEndTime(LocalDateTime.of(2026, 9, 3, 18, 0));
        item.setQuantity(1);

        RentOrderCreateBo createBo = new RentOrderCreateBo();
        createBo.setItems(Collections.singletonList(item));
        createBo.setPickupType(PickupType.STORE.getCode());
        createBo.setContactName("张三");
        createBo.setContactPhone("13800000000");

        RentOrderCreateVo vo = rentOrderService.createOrder(1L, createBo);
        assertEquals(OrderStatus.PENDING_PAYMENT.getCode(), vo.getStatus());
        verify(rentStockService).prelock(eq(100L), anyString(), eq(1L), eq(0L),
                any(LocalDateTime.class), any(LocalDateTime.class), eq(1));
    }

    @Test
    void createOrderShouldCalculateRentDepositDeliveryAndPayAmount() {
        RentGoods goods = onShelfGoods();
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);
        stubCreateOrderPricing(new BigDecimal("15.00"));
        doAnswer(invocation -> {
            RentOrder order = invocation.getArgument(0);
            order.setId(100L);
            return 1;
        }).when(rentOrderMapper).insert(any(RentOrder.class));

        RentOrderCreateItemBo item = new RentOrderCreateItemBo();
        item.setGoodsId(1L);
        item.setRentType("day");
        item.setStartTime(LocalDateTime.of(2026, 9, 1, 10, 0));
        item.setEndTime(LocalDateTime.of(2026, 9, 3, 18, 0));
        item.setQuantity(1);

        RentOrderCreateBo createBo = new RentOrderCreateBo();
        createBo.setItems(Collections.singletonList(item));
        createBo.setPickupType(PickupType.DELIVERY.getCode());
        createBo.setContactName("张三");
        createBo.setContactPhone("13800000000");

        RentOrderCreateVo vo = rentOrderService.createOrder(1L, createBo);

        ArgumentCaptor<RentOrder> orderCaptor = ArgumentCaptor.forClass(RentOrder.class);
        verify(rentOrderMapper).insert(orderCaptor.capture());
        RentOrder saved = orderCaptor.getValue();
        assertEquals(new BigDecimal("100.00"), saved.getRentAmount());
        assertEquals(new BigDecimal("200.00"), saved.getDepositAmount());
        assertEquals(new BigDecimal("15.00"), saved.getDeliveryAmount());
        assertEquals(new BigDecimal("315.00"), saved.getPayAmount());
        assertEquals(new BigDecimal("315.00"), vo.getPayAmount());
    }

    @Test
    void cancelOrderShouldReleaseStockWhenPendingPayment() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(rentOrderMapper.update(isNull(), any())).thenReturn(1);

        rentOrderService.cancelOrder(1L, 10L);

        verify(rentStockService).releaseOccupy(10L);
    }

    @Test
    void cancelOrderShouldRefundAndUnfreezeDepositWhenWaitingPickup() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.WAITING_PICKUP.getCode());
        order.setDepositAmount(new BigDecimal("200.00"));
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(rentOrderMapper.update(isNull(), any())).thenReturn(1);

        rentOrderService.cancelOrder(1L, 10L);

        verify(rentStockService).releaseOccupy(10L);
        verify(rentOrderRefundSupport).refundPaidOrder(eq(order), eq("用户取消订单（未取货）"));
        verify(userDepositService).unfreezeForCancelledOrder(1L, "ORD001", new BigDecimal("200.00"));
    }

    @Test
    void cancelOrderShouldRejectRentingStatus() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.RENTING.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BizException.class, () -> rentOrderService.cancelOrder(1L, 10L));
    }

    @Test
    void handlePaySuccessShouldConfirmOccupyAndRecordPayment() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setPickupType(PickupType.STORE.getCode());
        order.setPayAmount(new BigDecimal("315.00"));
        order.setEndTime(LocalDateTime.of(2026, 9, 3, 18, 0));
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(rentOrderMapper.update(isNull(), any())).thenReturn(1);

        rentOrderService.handlePaySuccess("ORD001", "wx", "TX001", new BigDecimal("315.00"));

        verify(rentStockService).confirmOccupy(10L);
        verify(rentPickupCodeMapper).insert(any(RentPickupCode.class));
        verify(rentOrderPaymentMapper).insert(any(RentOrderPayment.class));
    }

    @Test
    void handlePaySuccessShouldBeIdempotent() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setStatus(OrderStatus.WAITING_PICKUP.getCode());
        order.setPayAmount(new BigDecimal("100.00"));
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        rentOrderService.handlePaySuccess("ORD001", "wx", "TX001", new BigDecimal("100.00"));
        verify(rentStockService, never()).confirmOccupy(anyLong());
    }

    @Test
    void prepareRentOrderPayShouldReturnOrderAmount() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setPayAmount(new BigDecimal("100.00"));
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        BigDecimal amount = rentOrderService.prepareRentOrderPay(1L, "ORD001", new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), amount);
    }

    private static RentGoods onShelfGoods() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setName("测试帐篷");
        goods.setStatus(1);
        goods.setRentPriceDay(new BigDecimal("50.00"));
        goods.setDepositAmount(new BigDecimal("200.00"));
        goods.setCoverImage("/img/tent.jpg");
        return goods;
    }
}
