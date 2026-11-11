package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.domain.bo.UpdateUserBO;
import com.sqx.app.rental.user.domain.bo.AdminUserPageBo;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.app.rental.user.service.AdminRentalUserService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

/**
 * 管理端-C端用户（M05 增强）
 */
@Tag(name = "管理端-C端用户(M05)", description = "用户多维筛选列表与详情")
@RestController
@RequestMapping("/admin/user")
public class AdminRentalUserController {

    @Resource
    private AdminRentalUserService adminRentalUserService;

    @Operation(summary = "用户分页列表", description = "支持手机号/用户名/状态/会员/来源/邀请码/注册时间筛选")
    @PostMapping("/list")
    public Result<Page<AdminUserListVo>> list(@RequestBody @Validated AdminUserPageBo pageBo) {
        return Result.success(adminRentalUserService.page(pageBo));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/info")
    public Result<AppUserInfoVo> info(@NotNull(message = "用户id不能为空") @RequestParam("userId") Long userId) {
        return Result.success(adminRentalUserService.info(userId));
    }

    @Operation(summary = "修改用户")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Validated UpdateUserBO updateUserBO) {
        adminRentalUserService.update(updateUserBO);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody UpdateUserBO deleteBo) {
        adminRentalUserService.delete(deleteBo.getUserId());
        return Result.success();
    }
}
