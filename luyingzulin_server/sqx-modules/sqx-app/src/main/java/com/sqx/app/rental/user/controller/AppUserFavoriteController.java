package com.sqx.app.rental.user.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.UserFavoriteToggleBo;
import com.sqx.app.rental.user.domain.vo.UserFavoriteToggleVo;
import com.sqx.app.rental.user.domain.vo.UserFavoriteVo;
import com.sqx.app.rental.user.service.UserFavoriteService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户端-商品收藏
 */
@Tag(name = "用户端-商品收藏", description = "收藏列表、切换与校验")
@RestController
@RequestMapping("/app/user/favorite")
public class AppUserFavoriteController {

    @Resource
    private UserFavoriteService userFavoriteService;

    @Login
    @Operation(summary = "收藏列表")
    @GetMapping("/list")
    public Result<List<UserFavoriteVo>> list() {
        return Result.success(userFavoriteService.list(UserContextHolder.getAppUserId()));
    }

    @Login
    @Operation(summary = "切换收藏状态")
    @PostMapping("/toggle")
    public Result<UserFavoriteToggleVo> toggle(@RequestBody @Validated UserFavoriteToggleBo toggleBo) {
        return Result.success(userFavoriteService.toggle(UserContextHolder.getAppUserId(), toggleBo.getGoodsId()));
    }

    @Login
    @Operation(summary = "检查是否已收藏")
    @GetMapping("/check")
    public Result<Boolean> check(@RequestParam("goodsId") Long goodsId) {
        return Result.success(userFavoriteService.check(UserContextHolder.getAppUserId(), goodsId));
    }
}
