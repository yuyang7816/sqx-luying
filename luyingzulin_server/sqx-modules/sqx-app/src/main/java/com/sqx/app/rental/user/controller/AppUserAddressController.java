package com.sqx.app.rental.user.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.UserAddressSaveBo;
import com.sqx.app.rental.user.domain.vo.UserAddressVo;
import com.sqx.app.rental.user.service.UserAddressService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户端-配送地址
 */
@Tag(name = "用户端-配送地址", description = "地址 CRUD 与默认地址设置")
@RestController
@RequestMapping("/app/user/address")
public class AppUserAddressController {

    @Resource
    private UserAddressService userAddressService;

    @Login
    @Operation(summary = "地址列表")
    @GetMapping("/list")
    public Result<List<UserAddressVo>> list() {
        return Result.success(userAddressService.list(UserContextHolder.getAppUserId()));
    }

    @Login
    @Operation(summary = "地址详情")
    @GetMapping("/detail/{id}")
    public Result<UserAddressVo> detail(@PathVariable("id") Long id) {
        return Result.success(userAddressService.detail(UserContextHolder.getAppUserId(), id));
    }

    @Login
    @Operation(summary = "新增或更新地址")
    @PostMapping("/save")
    public Result<UserAddressVo> save(@RequestBody @Validated UserAddressSaveBo saveBo) {
        return Result.success(userAddressService.save(UserContextHolder.getAppUserId(), saveBo));
    }

    @Login
    @Operation(summary = "删除地址")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        userAddressService.delete(UserContextHolder.getAppUserId(), id);
        return Result.success();
    }

    @Login
    @Operation(summary = "设为默认地址")
    @PutMapping("/default/{id}")
    public Result<Void> setDefault(@PathVariable("id") Long id) {
        userAddressService.setDefault(UserContextHolder.getAppUserId(), id);
        return Result.success();
    }
}
