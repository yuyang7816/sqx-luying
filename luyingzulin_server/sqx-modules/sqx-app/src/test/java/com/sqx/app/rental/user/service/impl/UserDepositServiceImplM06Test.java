package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.user.domain.bo.UserDepositRefundApplyBo;
import com.sqx.app.rental.user.domain.entity.UserDeposit;
import com.sqx.app.rental.user.domain.entity.UserDepositLog;
import com.sqx.app.rental.user.mapper.UserDepositLogMapper;
import com.sqx.app.rental.user.mapper.UserDepositMapper;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.sqx.common.core.exception.BizException;
import com.sqx.system.db.entity.SysConfigEntity;
import com.sqx.system.db.service.ISysConfigService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * M06：押金冻结/退还与概览计算
 */
@ExtendWith(MockitoExtension.class)
class UserDepositServiceImplM06Test {

    @Mock
    private UserDepositMapper userDepositMapper;
    @Mock
    private UserDepositLogMapper userDepositLogMapper;
    @Mock
    private RentalWalletFlowService rentalWalletFlowService;
    @Mock
    private ISysConfigService iSysConfigService;
    @InjectMocks
    private UserDepositServiceImpl userDepositService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, UserDeposit.class);
        TableInfoHelper.initTableInfo(assistant, UserDepositLog.class);
    }

    @Test
    void summaryShouldComputeAvailableAmount() {
        UserDeposit deposit = new UserDeposit();
        deposit.setUserId(1L);
        deposit.setTotalAmount(new BigDecimal("200.00"));
        deposit.setFrozenAmount(new BigDecimal("50.00"));
        deposit.setStatus(1);
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(userDepositLogMapper.selectList(any())).thenReturn(java.util.Collections.emptyList());
        when(iSysConfigService.getOne(any())).thenReturn(null);

        assertEquals(new BigDecimal("150.00"), userDepositService.summary(1L).getAvailableAmount());
    }

    @Test
    void freezeForOrderShouldOnlyFreezeAvailable() {
        UserDeposit deposit = buildDeposit(1L, "200.00", "0.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(userDepositMapper.freezeAvailable(1L, new BigDecimal("100.00"))).thenReturn(1);
        when(userDepositLogMapper.insert(any(UserDepositLog.class))).thenReturn(1);

        userDepositService.freezeForOrder(1L, "ORD001", new BigDecimal("100.00"));

        verify(userDepositMapper).freezeAvailable(1L, new BigDecimal("100.00"));
        verify(userDepositMapper, never()).incrTotal(anyLong(), any());
        verify(rentalWalletFlowService).recordDepositFreeze(eq(1L), isNull(), eq("ORD001"), eq(new BigDecimal("100.00")));
    }

    @Test
    void freezeForOrderShouldSkipWhenAmountNotPositive() {
        userDepositService.freezeForOrder(1L, "ORD001", BigDecimal.ZERO);
        verifyNoInteractions(userDepositMapper, rentalWalletFlowService);
    }

    @Test
    void unfreezeForCancelledOrderShouldOnlyDecrFrozen() {
        when(userDepositLogMapper.insert(any(UserDepositLog.class))).thenReturn(1);

        userDepositService.unfreezeForCancelledOrder(1L, "ORD001", new BigDecimal("200.00"));

        verify(userDepositMapper).decrFrozen(1L, new BigDecimal("200.00"));
        verify(userDepositMapper, never()).decrTotal(anyLong(), any());
    }

    @Test
    void refundAfterInspectionShouldUnfreezeAndRefund() {
        when(userDepositLogMapper.insert(any(UserDepositLog.class))).thenReturn(1);

        userDepositService.refundAfterInspection(1L, "ORD001",
                new BigDecimal("80.00"), new BigDecimal("20.00"));

        verify(userDepositMapper).decrFrozen(1L, new BigDecimal("100.00"));
        verify(userDepositMapper).decrTotal(1L, new BigDecimal("80.00"));
        verify(rentalWalletFlowService).recordDepositRefund(eq(1L), isNull(), eq("ORD001"), eq(new BigDecimal("80.00")));
    }

    @Test
    void applyRefundShouldFreezeAvailableInsteadOfRefundFrozen() {
        UserDeposit deposit = buildDeposit(1L, "100.00", "20.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(userDepositLogMapper.selectCount(any())).thenReturn(0L);
        when(userDepositMapper.freezeAvailable(1L, new BigDecimal("50.00"))).thenReturn(1);
        when(userDepositLogMapper.insert(any(UserDepositLog.class))).thenReturn(1);

        UserDepositRefundApplyBo refundBo = new UserDepositRefundApplyBo();
        refundBo.setAmount(new BigDecimal("50.00"));

        userDepositService.applyRefund(1L, refundBo);

        verify(userDepositMapper).freezeAvailable(1L, new BigDecimal("50.00"));
        verify(userDepositMapper, never()).refundFrozen(anyLong(), any());
    }

    @Test
    void applyRefundShouldRejectWhenInsufficientAvailable() {
        UserDeposit deposit = buildDeposit(1L, "100.00", "80.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(userDepositLogMapper.selectCount(any())).thenReturn(0L);

        UserDepositRefundApplyBo refundBo = new UserDepositRefundApplyBo();
        refundBo.setAmount(new BigDecimal("50.00"));

        BizException ex = assertThrows(BizException.class, () -> userDepositService.applyRefund(1L, refundBo));
        assertTrue(ex.getMessage().contains("可退押金不足"));
    }

    @Test
    void assertSufficientForOrderShouldAllowWhenAvailableCoversOrderAndMinBalance() {
        UserDeposit deposit = buildDeposit(1L, "200.00", "0.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(iSysConfigService.getOne(any())).thenReturn(buildMinDepositConfig("200"));

        assertDoesNotThrow(() -> userDepositService.assertSufficientForOrder(1L, new BigDecimal("200.00")));
    }

    @Test
    void assertSufficientForOrderShouldRejectWhenAvailableBelowOrderDeposit() {
        UserDeposit deposit = buildDeposit(1L, "150.00", "0.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(iSysConfigService.getOne(any())).thenReturn(buildMinDepositConfig("200"));

        BizException ex = assertThrows(BizException.class,
                () -> userDepositService.assertSufficientForOrder(1L, new BigDecimal("200.00")));
        assertTrue(ex.getMessage().contains("可用押金不足"));
    }

    @Test
    void assertSufficientForOrderShouldRejectWhenAvailableBelowMinBalance() {
        UserDeposit deposit = buildDeposit(1L, "150.00", "0.00");
        when(userDepositMapper.selectOne(any())).thenReturn(deposit);
        when(iSysConfigService.getOne(any())).thenReturn(buildMinDepositConfig("200"));

        BizException ex = assertThrows(BizException.class,
                () -> userDepositService.assertSufficientForOrder(1L, new BigDecimal("100.00")));
        assertTrue(ex.getMessage().contains("可用押金需不低于"));
    }

    private static SysConfigEntity buildMinDepositConfig(String value) {
        SysConfigEntity config = new SysConfigEntity();
        config.setParamKey("user_min_deposit_balance");
        config.setParamValue(value);
        return config;
    }

    private static UserDeposit buildDeposit(Long userId, String total, String frozen) {
        UserDeposit deposit = new UserDeposit();
        deposit.setUserId(userId);
        deposit.setTotalAmount(new BigDecimal(total));
        deposit.setFrozenAmount(new BigDecimal(frozen));
        deposit.setStatus(1);
        return deposit;
    }
}
