package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.order.domain.bo.RentReturnApplyBo;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.common.core.exception.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M04：归还申请 renting→returning 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentReturnServiceImplM04Test {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, RentOrder.class);
        TableInfoHelper.initTableInfo(assistant, RentReturnApply.class);
    }

    @Mock
    private RentOrderMapper rentOrderMapper;
    @Mock
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Mock
    private RentOrderLogMapper rentOrderLogMapper;

    @InjectMocks
    private RentReturnServiceImpl rentReturnService;

    @Test
    void applyReturnShouldTransitionRentingToReturning() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.RENTING.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(rentReturnApplyMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(rentOrderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        RentReturnApplyBo applyBo = new RentReturnApplyBo();
        applyBo.setOrderId(10L);
        applyBo.setReturnMethod("store");
        applyBo.setReturnDate("2026-09-05");
        applyBo.setRemark("按时归还");

        rentReturnService.applyReturn(1L, applyBo);

        ArgumentCaptor<RentReturnApply> captor = ArgumentCaptor.forClass(RentReturnApply.class);
        verify(rentReturnApplyMapper).insert(captor.capture());
        assertEquals("store", captor.getValue().getReturnMethod());
        assertEquals("pending", captor.getValue().getStatus());
        verify(rentOrderMapper).update(any(), any(LambdaUpdateWrapper.class));
        verify(rentOrderLogMapper).insert(any());
    }

    @Test
    void applyReturnShouldRejectNonRentingOrder() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.WAITING_PICKUP.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        RentReturnApplyBo applyBo = new RentReturnApplyBo();
        applyBo.setOrderId(10L);
        applyBo.setReturnMethod("store");
        applyBo.setReturnDate("2026-09-05");

        assertThrows(BizException.class, () -> rentReturnService.applyReturn(1L, applyBo));
    }
}
