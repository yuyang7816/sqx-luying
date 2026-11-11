package com.sqx.app.rental.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.order.domain.bo.RentReturnApplyBo;
import com.sqx.app.rental.order.domain.vo.RentReturnDetailVo;
import com.sqx.app.rental.order.service.RentReturnService;
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
 * 用户端-归还
 */
@Tag(name = "用户端-归还", description = "归还申请与详情")
@RestController
@RequestMapping("/app/rental/return")
public class AppRentReturnController {

    @Resource
    private RentReturnService rentReturnService;

    @Login
    @Operation(summary = "提交归还申请", description = "renting → returning")
    @PostMapping("/apply")
    public Result<Void> applyReturn(@RequestBody @Validated RentReturnApplyBo applyBo) {
        Long userId = UserContextHolder.getAppUserId();
        rentReturnService.applyReturn(userId, applyBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "归还详情", description = "归还申请信息")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentReturnDetailVo.class)))
    @GetMapping("/detail/{orderId}")
    public Result<RentReturnDetailVo> returnDetail(@PathVariable("orderId") Long orderId) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentReturnService.returnDetail(userId, orderId));
    }
}
