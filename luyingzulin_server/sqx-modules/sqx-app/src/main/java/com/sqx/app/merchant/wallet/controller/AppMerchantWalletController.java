package com.sqx.app.merchant.wallet.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositLogPageBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositPayBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundApplyBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantWalletTransactionPageBo;
import com.sqx.app.merchant.wallet.domain.vo.*;
import com.sqx.app.merchant.wallet.service.MerchantWalletService;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordPageBo;
import com.sqx.app.referral.withdraw.domain.bo.WithdrawRecordSaveBo;
import com.sqx.app.referral.withdraw.domain.vo.WithdrawRecordVo;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

/**
 * 商户端-经营钱包与保证金（与用户个人钱包、租赁押金分离）
 */
@Tag(name = "商户端-经营钱包", description = "商户经营钱包、提现、商户保证金")
@RestController
@RequestMapping("/app/merchant/wallet")
public class AppMerchantWalletController {

    @Resource
    private MerchantWalletService merchantWalletService;

    @Login
    @Operation(summary = "商户钱包概览")
    @GetMapping("/summary")
    public Result<MerchantWalletSummaryVo> summary() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.summary(userId));
    }

    @Login
    @Operation(summary = "经营资金明细分页")
    @PostMapping("/transaction/page")
    public Result<IPage<MerchantWalletTransactionVo>> transactionPage(@RequestBody @Validated MerchantWalletTransactionPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.transactionPage(userId, pageBo));
    }

    @Login
    @Operation(summary = "商户钱包发起提现")
    @PostMapping("/withdraw/save")
    public Result<Void> withdrawSave(@RequestBody @Validated WithdrawRecordSaveBo saveBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantWalletService.withdraw(userId, saveBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "商户钱包提现记录")
    @PostMapping("/withdraw/page")
    public Result<IPage<WithdrawRecordVo>> withdrawPage(@RequestBody @Validated WithdrawRecordPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.withdrawPage(userId, pageBo));
    }

    @Login
    @Operation(summary = "商户保证金概览")
    @GetMapping("/security-deposit/summary")
    public Result<MerchantSecurityDepositSummaryVo> securityDepositSummary() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.securityDepositSummary(userId));
    }

    @Login
    @Operation(summary = "商户保证金流水分页")
    @PostMapping("/security-deposit/log/page")
    public Result<IPage<MerchantSecurityDepositLogVo>> securityDepositLogPage(@RequestBody @Validated MerchantSecurityDepositLogPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.securityDepositLogPage(userId, pageBo));
    }

    @Login
    @Operation(summary = "缴纳商户保证金")
    @PostMapping("/security-deposit/pay")
    public Result<Map<String, String>> securityDepositPay(@RequestBody @Validated MerchantSecurityDepositPayBo payBo) {
        Long userId = UserContextHolder.getAppUserId();
        String payNo = merchantWalletService.securityDepositPay(userId, payBo);
        return Result.success(Collections.singletonMap("payNo", payNo));
    }

    @Login
    @Operation(summary = "商户保证金支付状态")
    @GetMapping("/security-deposit/pay-status/{payNo}")
    public Result<MerchantSecurityDepositPayStatusVo> securityDepositPayStatus(@PathVariable("payNo") String payNo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantWalletService.securityDepositPayStatus(userId, payNo));
    }

    @Login
    @Operation(summary = "取消商户保证金待支付单")
    @PostMapping("/security-deposit/pay/cancel/{payNo}")
    public Result<Void> securityDepositPayCancel(@PathVariable("payNo") String payNo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantWalletService.securityDepositPayCancel(userId, payNo);
        return Result.success();
    }

    @Login
    @Operation(summary = "申请退还商户保证金")
    @PostMapping("/security-deposit/refund/apply")
    public Result<Void> securityDepositRefundApply(@RequestBody @Validated MerchantSecurityDepositRefundApplyBo refundBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantWalletService.securityDepositRefundApply(userId, refundBo);
        return Result.success();
    }
}
