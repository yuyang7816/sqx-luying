package com.sqx.app.rental.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.order.domain.bo.RentOrderPageBo;
import com.sqx.app.rental.order.domain.vo.RentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderListVo;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-租赁订单
 */
@Tag(name = "管理端-租赁订单", description = "订单列表、详情与代取消")
@RestController
@RequestMapping("/admin/rental/order")
public class AdminRentOrderController {

    @Resource
    private RentOrderService rentOrderService;

    @Operation(summary = "订单分页", description = "支持状态、关键词（订单号/手机号/商品名）、下单时间筛选")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderListVo.class)))
    @PostMapping("/page")
    public Result<IPage<RentOrderListVo>> page(@RequestBody @Validated RentOrderPageBo pageBo) {
        return Result.success(rentOrderService.adminPage(pageBo));
    }

    @Operation(summary = "订单详情", description = "含用户、商品明细、费用与时间轴")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentOrderDetailVo.class)))
    @GetMapping("/detail/{id}")
    public Result<RentOrderDetailVo> detail(@PathVariable("id") Long id) {
        return Result.success(rentOrderService.adminDetail(id));
    }

    @Operation(summary = "代取消订单", description = "仅 pending_payment 可取消，释放库存预占")
    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        rentOrderService.adminCancelOrder(id);
        return Result.success();
    }
}
