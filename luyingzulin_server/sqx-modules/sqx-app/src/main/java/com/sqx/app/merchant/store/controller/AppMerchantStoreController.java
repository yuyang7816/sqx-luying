package com.sqx.app.merchant.store.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.store.domain.bo.MerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.vo.MerchantStoreDetailVo;
import com.sqx.app.merchant.store.service.MerchantStoreService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-门店设置
 */
@Tag(name = "商户端-门店", description = "门店详情与保存")
@RestController
@RequestMapping("/app/merchant/store")
public class AppMerchantStoreController {

    @Resource
    private MerchantStoreService merchantStoreService;

    @Login
    @Operation(summary = "门店详情（含员工列表）")
    @GetMapping("/detail")
    public Result<MerchantStoreDetailVo> detail() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantStoreService.detail(userId));
    }

    @Login
    @Operation(summary = "保存门店")
    @PutMapping("/save")
    public Result<Void> save(@RequestBody @Validated MerchantStoreSaveBo saveBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantStoreService.save(userId, saveBo);
        return Result.success();
    }
}
