package com.sqx.app.rental.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.order.domain.vo.RentPickupCodeVo;
import com.sqx.app.rental.order.service.RentPickupService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户端-取货
 */
@Tag(name = "用户端-取货", description = "取货凭证与确认取货")
@RestController
@RequestMapping("/app/rental/pickup")
public class AppRentPickupController {

    @Resource
    private RentPickupService rentPickupService;

    @Login
    @Operation(summary = "取货凭证", description = "提货码、门店信息")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentPickupCodeVo.class)))
    @GetMapping("/code/{orderId}")
    public Result<RentPickupCodeVo> pickupCode(@PathVariable("orderId") Long orderId) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentPickupService.getPickupCode(userId, orderId));
    }

    @Login
    @Operation(summary = "确认取货", description = "waiting_pickup/delivering → renting")
    @PostMapping("/confirm/{orderId}")
    public Result<Void> confirmPickup(@PathVariable("orderId") Long orderId) {
        Long userId = UserContextHolder.getAppUserId();
        rentPickupService.confirmPickup(userId, orderId);
        return Result.success();
    }
}
