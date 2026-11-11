package com.sqx.app.merchant.dashboard.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardRevenueVo;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardStatsVo;
import com.sqx.app.merchant.dashboard.service.MerchantDashboardService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 商户端-工作台统计
 */
@Tag(name = "商户端-工作台", description = "商户经营数据")
@RestController
@RequestMapping("/app/merchant/dashboard")
public class AppMerchantDashboardController {

    @Resource
    private MerchantDashboardService merchantDashboardService;

    @Login
    @Operation(summary = "工作台统计")
    @GetMapping("/stats")
    public Result<MerchantDashboardStatsVo> stats() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantDashboardService.stats(userId));
    }

    @Login
    @Operation(summary = "营业额筛选")
    @GetMapping("/revenue")
    public Result<MerchantDashboardRevenueVo> revenue(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantDashboardService.revenue(userId, year, month));
    }
}
