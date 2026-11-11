package com.sqx.app.rental.dashboard.controller;

import com.sqx.app.rental.dashboard.domain.vo.AdminRentDashboardStatsVo;
import com.sqx.app.rental.dashboard.service.AdminRentDashboardService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 管理端-租赁看板
 */
@Tag(name = "管理端-租赁看板", description = "租赁业务统计")
@RestController
@RequestMapping("/admin/rental/dashboard")
public class AdminRentDashboardController {

    @Resource
    private AdminRentDashboardService adminRentDashboardService;

    @Operation(summary = "看板统计", description = "分类/商品/订单/分销等综合指标")
    @GetMapping("/stats")
    public Result<AdminRentDashboardStatsVo> stats() {
        return Result.success(adminRentDashboardService.stats());
    }
}
