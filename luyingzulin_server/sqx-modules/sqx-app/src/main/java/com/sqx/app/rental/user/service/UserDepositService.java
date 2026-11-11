package com.sqx.app.rental.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.*;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositRefundVo;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositVo;
import com.sqx.app.rental.user.domain.vo.UserDepositLogVo;
import com.sqx.app.rental.user.domain.vo.UserDepositPayStatusVo;
import com.sqx.app.rental.user.domain.vo.UserDepositSummaryVo;

import java.math.BigDecimal;

public interface UserDepositService {

    UserDepositSummaryVo summary(Long userId);

    IPage<UserDepositLogVo> logPage(Long userId, UserDepositLogPageBo pageBo);

    String applyPay(Long userId, UserDepositPayBo payBo);

    BigDecimal preparePay(Long userId, String payNo, BigDecimal amount);

    UserDepositPayStatusVo payStatus(Long userId, String payNo);

    void applyRefund(Long userId, UserDepositRefundApplyBo refundBo);

    void onDepositPaySuccess(Long userId, String payNo, BigDecimal amount);

    void assertSufficientForOrder(Long userId, BigDecimal requiredAmount);

    void freezeForOrder(Long userId, String orderNo, BigDecimal amount);

    void unfreezeForCancelledOrder(Long userId, String orderNo, BigDecimal amount);

    void refundAfterInspection(Long userId, String orderNo, BigDecimal refundAmount, BigDecimal deductAmount);

    IPage<AdminUserDepositVo> adminPage(AdminUserDepositPageBo pageBo);

    void adminRefund(AdminUserDepositRefundBo refundBo);

    void adminAdjust(AdminUserDepositAdjustBo adjustBo);

    IPage<AdminUserDepositRefundVo> adminRefundPage(AdminUserDepositRefundPageBo pageBo);

    void adminAuditRefund(AdminUserDepositRefundAuditBo auditBo);

    int countPendingRefund();
}
