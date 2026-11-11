package com.sqx.app.wallet.wallet.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.app.wallet.wallet.enums.WalletBizType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M06：钱包流水业务类型与方向
 */
@ExtendWith(MockitoExtension.class)
class RentalWalletFlowServiceM06Test {

    @Mock
    private IUserWalletService iUserWalletService;
    @Mock
    private IUserWalletTransactionService iUserWalletTransactionService;
    @InjectMocks
    private RentalWalletFlowService rentalWalletFlowService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                UserWallet.class);
    }

    @Test
    void recordDepositFreezeShouldWriteExpenseWithDepositFreezeBizType() {
        stubWallet(new BigDecimal("0.00"));

        rentalWalletFlowService.recordDepositFreeze(1L, 9L, "ORD001", new BigDecimal("200.00"));

        UserWalletTransaction tx = captureSavedTransaction();
        assertEquals(2, tx.getDirection());
        assertEquals(WalletBizType.DEPOSIT_FREEZE.getCode(), tx.getBizType());
        assertEquals("押金冻结", tx.getTitle());
    }

    @Test
    void recordDepositRefundShouldWriteIncomeWithDepositRefundBizType() {
        stubWallet(new BigDecimal("200.00"));

        rentalWalletFlowService.recordDepositRefund(1L, 9L, "ORD001", new BigDecimal("80.00"));

        UserWalletTransaction tx = captureSavedTransaction();
        assertEquals(1, tx.getDirection());
        assertEquals(WalletBizType.DEPOSIT_REFUND.getCode(), tx.getBizType());
        assertEquals("押金退还", tx.getTitle());
    }

    @Test
    void recordWithdrawShouldWriteExpenseWithWithdrawBizType() {
        rentalWalletFlowService.recordWithdraw(1L, 5L, new BigDecimal("100.00"));

        UserWalletTransaction tx = captureSavedTransaction();
        assertEquals(2, tx.getDirection());
        assertEquals(WalletBizType.WITHDRAW.getCode(), tx.getBizType());
        assertEquals("余额提现", tx.getTitle());
    }

    @Test
    void recordWithdrawRejectRefundShouldWriteIncomeWithRejectBizType() {
        rentalWalletFlowService.recordWithdrawRejectRefund(1L, 5L, new BigDecimal("100.00"));

        verify(iUserWalletService).incr(1L, new BigDecimal("100.00"));
        UserWalletTransaction tx = captureSavedTransaction();
        assertEquals(1, tx.getDirection());
        assertEquals(WalletBizType.WITHDRAW_REJECT.getCode(), tx.getBizType());
        assertEquals("提现驳回返还", tx.getTitle());
    }

    private void stubWallet(BigDecimal frozenAmount) {
        UserWallet wallet = new UserWallet();
        wallet.setId(1L);
        wallet.setUserId(1L);
        wallet.setAmount(new BigDecimal("500.00"));
        wallet.setFrozenAmount(frozenAmount);
        when(iUserWalletService.getOne(any())).thenReturn(wallet);
        when(iUserWalletService.updateById(any(UserWallet.class))).thenReturn(true);
    }

    private UserWalletTransaction captureSavedTransaction() {
        ArgumentCaptor<UserWalletTransaction> captor = ArgumentCaptor.forClass(UserWalletTransaction.class);
        verify(iUserWalletTransactionService).save(captor.capture());
        return captor.getValue();
    }
}
