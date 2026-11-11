package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.UserDepositLogPageBo;
import com.sqx.app.rental.user.domain.bo.UserDepositPayBo;
import com.sqx.app.rental.user.domain.bo.UserDepositRefundApplyBo;
import com.sqx.app.rental.user.domain.vo.UserDepositLogVo;
import com.sqx.app.rental.user.domain.vo.UserDepositPayStatusVo;
import com.sqx.app.rental.user.domain.vo.UserDepositSummaryVo;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

@Tag(name = "用户端-押金", description = "押金概览、流水、缴纳与退押申请")
@RestController
@RequestMapping("/app/user/deposit")
public class AppUserDepositController {

    @Resource
    private UserDepositService userDepositService;

    @Login
    @Operation(summary = "押金概览")
    @GetMapping("/summary")
    public Result<UserDepositSummaryVo> summary() {
        return Result.success(userDepositService.summary(UserContextHolder.getAppUserId()));
    }

    @Login
    @Operation(summary = "押金流水分页")
    @PostMapping("/log/page")
    public Result<IPage<UserDepositLogVo>> logPage(@RequestBody @Validated UserDepositLogPageBo pageBo) {
        return Result.success(userDepositService.logPage(UserContextHolder.getAppUserId(), pageBo));
    }

    @Login
    @Operation(summary = "缴纳押金（返回支付单号，对接 DEPOSIT_PAY）")
    @PostMapping("/pay")
    public Result<Map<String, String>> pay(@RequestBody @Validated UserDepositPayBo payBo) {
        String payNo = userDepositService.applyPay(UserContextHolder.getAppUserId(), payBo);
        return Result.success(Collections.singletonMap("payNo", payNo));
    }

    @Login
    @Operation(summary = "押金缴纳支付状态")
    @GetMapping("/pay-status/{payNo}")
    public Result<UserDepositPayStatusVo> payStatus(@PathVariable("payNo") String payNo) {
        return Result.success(userDepositService.payStatus(UserContextHolder.getAppUserId(), payNo));
    }

    @Login
    @Operation(summary = "申请退押")
    @PostMapping("/refund/apply")
    public Result<Void> refundApply(@RequestBody @Validated UserDepositRefundApplyBo refundBo) {
        userDepositService.applyRefund(UserContextHolder.getAppUserId(), refundBo);
        return Result.success();
    }
}
