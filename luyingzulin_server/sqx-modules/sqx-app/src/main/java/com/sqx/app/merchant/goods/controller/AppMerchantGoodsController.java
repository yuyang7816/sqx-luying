package com.sqx.app.merchant.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsPageBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsShelfBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantStockAdjustBo;
import com.sqx.app.merchant.goods.service.MerchantGoodsService;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-商品管理
 */
@Tag(name = "商户端-商品", description = "商户商品分页、保存、上下架")
@RestController
@RequestMapping("/app/merchant/goods")
public class AppMerchantGoodsController {

    @Resource
    private MerchantGoodsService merchantGoodsService;

    @Login
    @Operation(summary = "商品分页")
    @PostMapping("/page")
    public Result<IPage<RentGoodsListVo>> page(@RequestBody @Validated MerchantGoodsPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantGoodsService.page(userId, pageBo));
    }

    @Login
    @Operation(summary = "商品详情")
    @GetMapping("/detail/{id}")
    public Result<RentGoodsDetailVo> detail(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantGoodsService.detail(userId, id));
    }

    @Login
    @Operation(summary = "保存商品（新增或编辑）")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated RentGoodsUpdateBo saveBo) {
        Long userId = UserContextHolder.getAppUserId();
        if (saveBo.getId() != null) {
            merchantGoodsService.update(userId, saveBo);
        } else {
            merchantGoodsService.save(userId, saveBo);
        }
        return Result.success();
    }

    @Login
    @Operation(summary = "编辑商品（兼容旧客户端）")
    @PutMapping("/save")
    public Result<Void> update(@RequestBody @Validated RentGoodsUpdateBo updateBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantGoodsService.update(userId, updateBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "上下架")
    @PutMapping("/shelf")
    public Result<Void> shelf(@RequestBody @Validated MerchantGoodsShelfBo shelfBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantGoodsService.shelf(userId, shelfBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        Long userId = UserContextHolder.getAppUserId();
        merchantGoodsService.delete(userId, id);
        return Result.success();
    }
}
