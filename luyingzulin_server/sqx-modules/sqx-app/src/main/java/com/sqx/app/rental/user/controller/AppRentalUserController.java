package com.sqx.app.rental.user.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.UserProfileUpdateBo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.app.rental.user.service.RentalUserService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户端-个人资料（M05）
 */
@Tag(name = "用户端-个人资料", description = "用户信息查询与资料修改")
@RestController
@RequestMapping("/app/user")
public class AppRentalUserController {

    @Resource
    private RentalUserService rentalUserService;

    @Login
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<AppUserInfoVo> info() {
        return Result.success(rentalUserService.getInfo(UserContextHolder.getAppUserId()));
    }

    @Login
    @Operation(summary = "更新个人资料")
    @PutMapping("/profile")
    public Result<AppUserInfoVo> updateProfile(@RequestBody @Validated UserProfileUpdateBo updateBo) {
        return Result.success(rentalUserService.updateProfile(UserContextHolder.getAppUserId(), updateBo));
    }
}
