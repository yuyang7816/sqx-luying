package com.sqx.app.wallet.wallet.service.impl;

import com.sqx.app.base.pay.business.service.BusinessService;
import com.sqx.app.base.pay.domain.BusinessParam;
import com.sqx.app.base.pay.enums.BusinessType;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.app.rental.order.service.RentRenewService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.bo.UserWalletPayBalanceBo;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWalletServiceImplTest {

    @InjectMocks
    private UserWalletServiceImpl userWalletService;

    @Mock
    private IUserWalletService iUserWalletService;
    @Mock
    private IUserWalletTransactionService iUserWalletTransactionService;
    @Mock
    private BusinessService businessService;
    @Mock
    private RentOrderService rentOrderService;
    @Mock
    private RentRenewService rentRenewService;
    @Mock
    private UserDepositService userDepositService;
    @Mock
    private VipOrderService vipOrderService;

    @Test
    void payWithBalanceShouldCompleteRentOrderPay() {
        try (MockedStatic<RequestContextHolder> requestContext = mockStatic(RequestContextHolder.class)) {
            RequestAttributes attributes = mock(RequestAttributes.class);
            requestContext.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getAttribute("userId", RequestAttributes.SCOPE_REQUEST)).thenReturn("1");

            UserWallet wallet = new UserWallet();
            wallet.setUserId(1L);
            wallet.setAmount(new BigDecimal("200.00"));
            when(iUserWalletService.getOne(any())).thenReturn(wallet);
            when(iUserWalletService.decr(eq(1L), eq(new BigDecimal("99.00")))).thenReturn(1);
            doAnswer(invocation -> {
                BusinessParam param = invocation.getArgument(0);
                param.setAmount(new BigDecimal("99.00"));
                return null;
            }).when(businessService).process(any(BusinessParam.class));

            UserWalletPayBalanceBo bo = new UserWalletPayBalanceBo();
            bo.setOrderNo("202608311323309184");
            bo.setBusinessType(BusinessType.RENT_ORDER.getCode());

            userWalletService.payWithBalance(bo);

            verify(rentOrderService).handlePaySuccess(eq("202608311323309184"), eq("balance"),
                    any(String.class), eq(new BigDecimal("99.00")));
            verify(iUserWalletTransactionService).save(any());
        }
    }

    @Test
    void payWithBalanceShouldRejectWhenDecrFailed() {
        try (MockedStatic<RequestContextHolder> requestContext = mockStatic(RequestContextHolder.class)) {
            RequestAttributes attributes = mock(RequestAttributes.class);
            requestContext.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            when(attributes.getAttribute("userId", RequestAttributes.SCOPE_REQUEST)).thenReturn("1");

            UserWallet wallet = new UserWallet();
            wallet.setUserId(1L);
            wallet.setAmount(new BigDecimal("200.00"));
            when(iUserWalletService.getOne(any())).thenReturn(wallet);
            when(iUserWalletService.decr(eq(1L), eq(new BigDecimal("99.00")))).thenReturn(0);
            doAnswer(invocation -> {
                BusinessParam param = invocation.getArgument(0);
                param.setAmount(new BigDecimal("99.00"));
                return null;
            }).when(businessService).process(any(BusinessParam.class));

            UserWalletPayBalanceBo bo = new UserWalletPayBalanceBo();
            bo.setOrderNo("202608311323309184");
            bo.setBusinessType(BusinessType.RENT_ORDER.getCode());

            BizException ex = assertThrows(BizException.class, () -> userWalletService.payWithBalance(bo));
            assertEquals("余额不足", ex.getMessage());
            verify(rentOrderService, never()).handlePaySuccess(any(), any(), any(), any());
        }
    }
}
