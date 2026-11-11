package com.sqx.app.merchant.store.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStorePageBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreUpdateStatusBo;
import com.sqx.app.merchant.store.domain.vo.AdminMerchantStoreListVo;
import com.sqx.app.merchant.store.service.AdminMerchantStoreService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-商户门店
 */
@Tag(name = "管理端-商户门店", description = "门店维护与启停")
@RestController
@RequestMapping("/admin/merchant/store")
public class AdminMerchantStoreController {

    @Resource
    private AdminMerchantStoreService adminMerchantStoreService;

    @Operation(summary = "门店分页")
    @PostMapping("/page")
    public Result<IPage<AdminMerchantStoreListVo>> page(@RequestBody @Validated AdminMerchantStorePageBo pageBo) {
        return Result.success(adminMerchantStoreService.adminPage(pageBo));
    }

    @Operation(summary = "保存门店", description = "新增或编辑")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated AdminMerchantStoreSaveBo saveBo) {
        adminMerchantStoreService.adminSave(saveBo);
        return Result.success();
    }

    @Operation(summary = "启用/停用门店")
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody @Validated AdminMerchantStoreUpdateStatusBo updateStatusBo) {
        adminMerchantStoreService.adminUpdateStatus(updateStatusBo);
        return Result.success();
    }
}
