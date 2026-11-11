package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentPickupCode;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentPickupCodeMapper;
import com.sqx.common.core.exception.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M04：取货确认 pickup→renting 单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentPickupServiceImplM04Test {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration config = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(config, "");
        TableInfoHelper.initTableInfo(assistant, RentOrder.class);
        TableInfoHelper.initTableInfo(assistant, RentPickupCode.class);
    }

    @Mock
    private RentOrderMapper rentOrderMapper;
    @Mock
    private RentPickupCodeMapper rentPickupCodeMapper;
    @Mock
    private RentOrderLogMapper rentOrderLogMapper;

    @InjectMocks
    private RentPickupServiceImpl rentPickupService;

    @Test
    void confirmPickupShouldTransitionWaitingPickupToRenting() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.WAITING_PICKUP.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);
        when(rentOrderMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(rentPickupCodeMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        rentPickupService.confirmPickup(1L, 10L);

        verify(rentOrderMapper).update(any(), any(LambdaUpdateWrapper.class));
        verify(rentPickupCodeMapper).update(any(), any(LambdaUpdateWrapper.class));
        verify(rentOrderLogMapper).insert(any());
    }

    @Test
    void confirmPickupShouldRejectRentingStatus() {
        RentOrder order = new RentOrder();
        order.setId(10L);
        order.setOrderNo("ORD001");
        order.setUserId(1L);
        order.setStatus(OrderStatus.RENTING.getCode());
        when(rentOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertThrows(BizException.class, () -> rentPickupService.confirmPickup(1L, 10L));
    }
}
