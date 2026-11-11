package com.sqx.app.rental.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.order.domain.bo.RentRenewCreateBo;
import com.sqx.app.rental.order.domain.vo.RentRenewCreateVo;
import com.sqx.app.rental.order.domain.vo.RentRenewPreviewVo;
import com.sqx.app.rental.order.service.RentRenewService;
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
 * 用户端-续租
 */
@Tag(name = "用户端-续租", description = "续租预览与创建")
@RestController
@RequestMapping("/app/rental/renew")
public class AppRentRenewController {

    @Resource
    private RentRenewService rentRenewService;

    @Login
    @Operation(summary = "续租预览", description = "计算续租租金与延长后租期")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentRenewPreviewVo.class)))
    @GetMapping("/preview/{orderId}")
    public Result<RentRenewPreviewVo> preview(@PathVariable("orderId") Long orderId,
                                              @RequestParam("newEndTime") String newEndTime) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentRenewService.preview(userId, orderId, newEndTime));
    }

    @Login
    @Operation(summary = "创建续租", description = "生成续租单，businessType=rent_renew 发起支付")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentRenewCreateVo.class)))
    @PostMapping("/create")
    public Result<RentRenewCreateVo> create(@RequestBody @Validated RentRenewCreateBo createBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentRenewService.create(userId, createBo));
    }
}
