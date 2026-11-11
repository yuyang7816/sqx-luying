package com.sqx.app.referral.withdraw.service.impl;

import com.sqx.app.referral.commission.db.service.IReferralCommissionService;
import com.sqx.app.referral.user.db.service.IReferralUserCommissionService;
import com.sqx.app.referral.withdraw.db.service.IWithdrawRecordService;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordConfirmBo;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawRecord;
import com.sqx.app.referral.withdraw.enums.WithdrawSourceType;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * M06：提现审核状态机
 */
@ExtendWith(MockitoExtension.class)
class WithdrawRecordServiceImplM06Test {

    @Mock
    private IWithdrawRecordService iWithdrawRecordService;
    @Mock
    private IReferralUserCommissionService iReferralUserCommissionService;
    @Mock
    private IReferralCommissionService iReferralCommissionService;
    @Mock
    private IUserWalletService iUserWalletService;
    @Mock
    private RentalWalletFlowService rentalWalletFlowService;
    @InjectMocks
    private WithdrawRecordServiceImpl withdrawRecordService;

    @Test
    void confirmShouldMarkSuccessWhenStatusIsTwo() {
        WithdrawRecord record = buildPendingRecord(WithdrawSourceType.WALLET.getCode(), "100.00");
        when(iWithdrawRecordService.getById(1L)).thenReturn(record);

        WithdrawRecordConfirmBo confirmBo = new WithdrawRecordConfirmBo();
        confirmBo.setId(1L);
        confirmBo.setStatus(2);

        withdrawRecordService.confirm(confirmBo);

        ArgumentCaptor<WithdrawRecord> captor = ArgumentCaptor.forClass(WithdrawRecord.class);
        verify(iWithdrawRecordService).updateById(captor.capture());
        assertEquals(2, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getWithdrawTime());
        verifyNoInteractions(iUserWalletService, rentalWalletFlowService);
    }

    @Test
    void confirmShouldRefundWalletWhenRejectWalletWithdraw() {
        WithdrawRecord record = buildPendingRecord(WithdrawSourceType.WALLET.getCode(), "100.00");
        when(iWithdrawRecordService.getById(2L)).thenReturn(record);

        WithdrawRecordConfirmBo confirmBo = new WithdrawRecordConfirmBo();
        confirmBo.setId(2L);
        confirmBo.setStatus(3);
        confirmBo.setReason("账户信息不符");

        withdrawRecordService.confirm(confirmBo);

        verify(iUserWalletService).incr(1L, new BigDecimal("100.00"));
        verify(rentalWalletFlowService).recordWithdrawRejectRefund(1L, 2L, new BigDecimal("100.00"));
        verify(iWithdrawRecordService).updateById(argThat(updated ->
                updated.getStatus() == 3 && "账户信息不符".equals(updated.getReason())));
    }

    @Test
    void confirmShouldRefundReferralWhenRejectReferralWithdraw() {
        WithdrawRecord record = buildPendingRecord(WithdrawSourceType.REFERRAL.getCode(), "50.00");
        when(iWithdrawRecordService.getById(3L)).thenReturn(record);

        WithdrawRecordConfirmBo confirmBo = new WithdrawRecordConfirmBo();
        confirmBo.setId(3L);
        confirmBo.setStatus(3);
        confirmBo.setReason("资料不全");

        withdrawRecordService.confirm(confirmBo);

        verify(iReferralUserCommissionService).incr(1L, new BigDecimal("50.00"));
        verify(iReferralCommissionService).saveRefuseWithdraw(1L, new BigDecimal("50.00"));
        verifyNoInteractions(iUserWalletService);
    }

    @Test
    void confirmShouldRejectIllegalStatus() {
        WithdrawRecord record = buildPendingRecord(WithdrawSourceType.WALLET.getCode(), "100.00");
        when(iWithdrawRecordService.getById(4L)).thenReturn(record);

        WithdrawRecordConfirmBo confirmBo = new WithdrawRecordConfirmBo();
        confirmBo.setId(4L);
        confirmBo.setStatus(1);

        BizException ex = assertThrows(BizException.class, () -> withdrawRecordService.confirm(confirmBo));
        assertTrue(ex.getMessage().contains("非法操作"));
    }

    private static WithdrawRecord buildPendingRecord(int sourceType, String amount) {
        WithdrawRecord record = new WithdrawRecord();
        record.setId(sourceType == WithdrawSourceType.WALLET.getCode() ? 2L : 3L);
        record.setUserId(1L);
        record.setSourceType(sourceType);
        record.setAmount(new BigDecimal(amount));
        record.setStatus(1);
        return record;
    }
}
