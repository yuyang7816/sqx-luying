package com.sqx.app.rental.cart.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.cart.domain.bo.*;
import com.sqx.app.rental.cart.domain.vo.RentCartCheckoutPreviewVo;
import com.sqx.app.rental.cart.domain.vo.RentCartItemVo;
import com.sqx.app.rental.cart.service.RentCartService;
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
import java.util.List;

/**
 * 用户端-租赁购物车
 */
@Tag(name = "用户端-租赁购物车", description = "购物车 CRUD 与结算预览")
@RestController
@RequestMapping("/app/rental/cart")
public class AppRentCartController {

    @Resource
    private RentCartService rentCartService;

    @Login
    @Operation(summary = "购物车列表", description = "含失效商品标识")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentCartItemVo.class)))
    @GetMapping("/list")
    public Result<List<RentCartItemVo>> list() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentCartService.list(userId));
    }

    @Login
    @Operation(summary = "加入购物车", description = "相同 SKU 合并数量")
    @PostMapping("/add")
    public Result<Long> add(@RequestBody @Validated RentCartAddBo addBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentCartService.add(userId, addBo));
    }

    @Login
    @Operation(summary = "更新购物车", description = "更新数量/规格/租期")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody @Validated RentCartUpdateBo updateBo) {
        Long userId = UserContextHolder.getAppUserId();
        rentCartService.update(userId, updateBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "删除购物车条目")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        rentCartService.delete(userId, id);
        return Result.success();
    }

    @Login
    @Operation(summary = "批量删除购物车")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(@RequestBody @Validated RentCartDeleteBo deleteBo) {
        Long userId = UserContextHolder.getAppUserId();
        rentCartService.deleteBatch(userId, deleteBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "勾选状态", description = "单选/批量/全选")
    @PutMapping("/select")
    public Result<Void> select(@RequestBody @Validated RentCartSelectBo selectBo) {
        Long userId = UserContextHolder.getAppUserId();
        rentCartService.select(userId, selectBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "结算预览", description = "返回 confirmPayload 供创建订单")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentCartCheckoutPreviewVo.class)))
    @PostMapping("/checkout-preview")
    public Result<RentCartCheckoutPreviewVo> checkoutPreview(
            @RequestBody @Validated RentCartCheckoutPreviewBo previewBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(rentCartService.checkoutPreview(userId, previewBo));
    }
}
