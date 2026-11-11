package com.sqx.app.rental.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.order.domain.bo.AppRentOrderPageBo;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateBo;
import com.sqx.app.rental.order.domain.vo.AppRentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderCreateVo;
import com.sqx.app.rental.order.domain.vo.RentOrderListVo;
import com.sqx.app.rental.order.domain.vo.RentOrderPayStatusVo;
import com.sqx.app.rental.order.domain.vo.RentOrderStatusCountVo;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户端-租赁订单
 */
@Tag(name = "用户端-租赁订单", description = "租赁订单创建、取消与支付状态")
@RestController
@RequestMapping("/app/rental/order")
public class AppRentOrderController {

    @Resource
    private RentOrderService rentOrderService;

    @Login
    @Operation(summary = "订单分页", description = "我的订单列表，支持 status Tab 筛选")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderListVo.class)))
    @PostMapping("/page")
    public Result<IPage<RentOrderListVo>> page(@RequestBody @Validated AppRentOrderPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentOrderService.appPage(userId, pageBo));
    }

    @Login
    @Operation(summary = "订单详情", description = "含商品明细、费用、时间轴、续租/归还/验收信息")
    @ApiResponse(content = @Content(schema = @Schema(implementation = AppRentOrderDetailVo.class)))
    @GetMapping("/detail/{id}")
    public Result<AppRentOrderDetailVo> detail(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentOrderService.appDetail(userId, id));
    }

    @Login
    @Operation(summary = "订单状态角标", description = "我的页待付款/待取货/租赁中/待归还/售后数量")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderStatusCountVo.class)))
    @GetMapping("/status-counts")
    public Result<RentOrderStatusCountVo> statusCounts() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentOrderService.statusCounts(userId));
    }

    @Login
    @Operation(summary = "创建订单", description = "状态 pending_payment，预占库存 PRELOCK")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderCreateVo.class)))
    @PostMapping("/create")
    public Result<RentOrderCreateVo> create(@RequestBody @Validated RentOrderCreateBo createBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentOrderService.createOrder(userId, createBo));
    }

    @Login
    @Operation(summary = "取消订单", description = "仅 pending_payment 可取消，释放库存预占")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        rentOrderService.cancelOrder(userId, id);
        return Result.success();
    }

    @Login
    @Operation(summary = "支付状态查询", description = "轮询支付结果")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderPayStatusVo.class)))
    @GetMapping("/pay-status/{orderNo}")
    public Result<RentOrderPayStatusVo> payStatus(@PathVariable("orderNo") String orderNo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentOrderService.payStatus(userId, orderNo));
    }
}
