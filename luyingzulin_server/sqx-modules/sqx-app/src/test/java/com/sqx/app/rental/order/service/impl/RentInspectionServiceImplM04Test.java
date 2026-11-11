package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.order.domain.bo.RentInspectionSubmitBo;
import com.sqx.app.rental.order.domain.entity.RentInspection;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.domain.vo.RentInspectionDetailVo;
import com.sqx.app.rental.order.mapper.RentInspectionMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.app.rental.order.support.RentOrderNotificationSupport;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.app.referral.user.service.ReferralUserCommissionService;
import com.sqx.app.rental.user.service.UserDepositService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M04：验收退押计算单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentInspectionServiceImplM04Test {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, RentOrder.class);
        TableInfoHelper.initTableInfo(assistant, RentInspection.class);
        TableInfoHelper.initTableInfo(assistant, RentReturnApply.class);
    }

    @Mock
    private RentOrderMapper rentOrderMapper;
    @Mock
    private RentInspectionMapper rentInspectionMapper;
    @Mock
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Mock
    private RentOrderLogMapper rentOrderLogMapper;
    @Mock
    private RentStockService rentStockService;
    @Mock
    private UserDepositService userDepositService;
    @Mock
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Mock
    private MerchantContextSupport merchantContextSupport;
    @Mock
    private RentOrderNotificationSupport rentOrderNotificationSupport;
    @Mock
    private ReferralUserCommissionService referralUserCommissionService;

    @InjectMocks
    private RentInspectionServiceImpl rentInspectionService;

    private void stubMerchantContext() {
        when(merchantContextSupport.requireMerchantContextForOperation(anyLong()))
                .thenReturn(new MerchantContextSupport.MerchantContext(1L, 99L, null));
    }

    @Test
    void merchantSubmitShouldCalculateRefundAsDepositMinusDeduct() {
        stubMerchantContext();
        RentOrder order = returningOrder(new BigDecimal("200.00"));
        when(rentOrderMapper.selectById(10L)).thenReturn(order);
        when(rentInspectionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(rentOrderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rentReturnApplyMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RentInspectionSubmitBo submitBo = new RentInspectionSubmitBo();
        submitBo.setOrderId(10L);
        submitBo.setDeviceCondition("minor_damage");
        submitBo.setDeductAmount(new BigDecimal("50.00"));
        submitBo.setRemark("轻微污损");
        submitBo.setImageUrls(java.util.Collections.singletonList("/upload/a.jpg"));

        rentInspectionService.merchantSubmit(99L, submitBo);

        ArgumentCaptor<RentInspection> captor = ArgumentCaptor.forClass(RentInspection.class);
        verify(rentInspectionMapper).insert(captor.capture());
        RentInspection saved = captor.getValue();
        assertEquals(new BigDecimal("50.00"), saved.getDeductAmount());
        assertEquals(new BigDecimal("150.00"), saved.getRefundAmount());
        verify(rentStockService).finishOccupy(10L);
    }

    @Test
    void merchantSubmitShouldRejectDeductExceedingDeposit() {
        stubMerchantContext();
        RentOrder order = returningOrder(new BigDecimal("100.00"));
        when(rentOrderMapper.selectById(10L)).thenReturn(order);
        when(rentInspectionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RentInspectionSubmitBo submitBo = new RentInspectionSubmitBo();
        submitBo.setOrderId(10L);
        submitBo.setDeductAmount(new BigDecimal("150.00"));
        submitBo.setRemark("损坏");

        assertThrows(com.sqx.common.core.exception.BizException.class,
                () -> rentInspectionService.merchantSubmit(99L, submitBo));
    }

    @Test
    void merchantSubmitShouldRequireRemark() {
        stubMerchantContext();
        RentOrder order = returningOrder(new BigDecimal("100.00"));
        when(rentOrderMapper.selectById(10L)).thenReturn(order);
        when(rentInspectionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RentInspectionSubmitBo submitBo = new RentInspectionSubmitBo();
        submitBo.setOrderId(10L);
        submitBo.setDeductAmount(new BigDecimal("20.00"));
        submitBo.setImageUrls(java.util.Collections.singletonList("/upload/a.jpg"));

        assertThrows(com.sqx.common.core.exception.BizException.class,
                () -> rentInspectionService.merchantSubmit(99L, submitBo));
    }

    @Test
    void merchantSubmitShouldRequireImageUrls() {
        stubMerchantContext();
        RentOrder order = returningOrder(new BigDecimal("100.00"));
        when(rentOrderMapper.selectById(10L)).thenReturn(order);
        when(rentInspectionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        RentInspectionSubmitBo submitBo = new RentInspectionSubmitBo();
        submitBo.setOrderId(10L);
        submitBo.setRemark("设备完好");

        assertThrows(com.sqx.common.core.exception.BizException.class,
                () -> rentInspectionService.merchantSubmit(99L, submitBo));
    }

    @Test
    void merchantSubmitShouldAllowFullDeductWithZeroRefund() {
        stubMerchantContext();
        RentOrder order = returningOrder(new BigDecimal("100.00"));
        when(rentOrderMapper.selectById(10L)).thenReturn(order);
        when(rentInspectionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(rentOrderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rentReturnApplyMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RentInspectionSubmitBo submitBo = new RentInspectionSubmitBo();
        submitBo.setOrderId(10L);
        submitBo.setDeductAmount(new BigDecimal("100.00"));
        submitBo.setRemark("设备严重损坏");
        submitBo.setImageUrls(java.util.Collections.singletonList("/upload/b.jpg"));

        rentInspectionService.merchantSubmit(99L, submitBo);

        ArgumentCaptor<RentInspection> captor = ArgumentCaptor.forClass(RentInspection.class);
        verify(rentInspectionMapper).insert(captor.capture());
        assertEquals(new BigDecimal("100.00"), captor.getValue().getDeductAmount());
        assertEquals(0, captor.getValue().getRefundAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    void toDetailVoShouldExposeDeductAndRefundAmounts() {
        RentInspection inspection = new RentInspection();
        inspection.setId(1L);
        inspection.setOrderId(10L);
        inspection.setOrderNo("ORD001");
        inspection.setDeviceCondition("good");
        inspection.setDeductAmount(new BigDecimal("30.00"));
        inspection.setRefundAmount(new BigDecimal("170.00"));
        inspection.setAuditStatus("approved");

        RentInspectionDetailVo vo = RentInspectionServiceImpl.toDetailVo(inspection, null);

        assertEquals(new BigDecimal("30.00"), vo.getDeductAmount());
        assertEquals(new BigDecimal("170.00"), vo.getRefundAmount());
    }

    private static RentOrder returningOrder(BigDecimal deposit) {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setMerchantId(1L);
        order.setUserId(1L);
        order.setStatus(OrderStatus.RETURNING.getCode());
        order.setDepositAmount(deposit);
        return order;
    }
}
