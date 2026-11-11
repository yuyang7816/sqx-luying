package com.sqx.app.merchant.staff.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.staff.domain.bo.MerchantStaffSaveBo;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffProfileVo;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffVo;
import com.sqx.app.merchant.staff.service.MerchantStaffService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 商户端-员工管理
 */
@Tag(name = "商户端-员工", description = "门店员工 CRUD")
@RestController
@RequestMapping("/app/merchant/staff")
public class AppMerchantStaffController {

    @Resource
    private MerchantStaffService merchantStaffService;

    @Login
    @Operation(summary = "当前用户员工身份")
    @GetMapping("/me")
    public Result<MerchantStaffProfileVo> profile() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantStaffService.profile(userId));
    }

    @Login
    @Operation(summary = "员工列表")
    @GetMapping("/list")
    public Result<List<MerchantStaffVo>> list() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantStaffService.list(userId));
    }

    @Login
    @Operation(summary = "新增/编辑员工")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated MerchantStaffSaveBo saveBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantStaffService.save(userId, saveBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "编辑员工")
    @PutMapping("/save")
    public Result<Void> update(@RequestBody @Validated MerchantStaffSaveBo saveBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantStaffService.save(userId, saveBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "删除员工")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        merchantStaffService.delete(userId, id);
        return Result.success();
    }
}
