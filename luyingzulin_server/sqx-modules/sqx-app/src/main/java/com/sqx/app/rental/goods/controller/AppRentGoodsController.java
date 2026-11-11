package com.sqx.app.rental.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.app.rental.goods.service.RentGoodsService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.core.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户端-租赁商品
 */
@Tag(name = "用户端-租赁商品", description = "租赁商品浏览")
@RestController
@RequestMapping("/app/rental/goods")
public class AppRentGoodsController {

    @Resource
    private RentGoodsService rentGoodsService;
    @Resource
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @Operation(summary = "商品分页", description = "支持分类、关键词、排序筛选")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentGoodsListVo.class)))
    @PostMapping("/page")
    public Result<IPage<RentGoodsListVo>> page(@RequestBody @Validated RentGoodsPageBo pageBo) {
        if (pageBo.getStatus() == null) {
            pageBo.setStatus(1);
        }
        pageBo.setOpenStoreOnly(true);
        return Result.success(rentGoodsService.page(pageBo));
    }

    @Operation(summary = "商品详情", description = "含规格、图片、库存概览")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentGoodsDetailVo.class)))
    @GetMapping("/detail/{id}")
    public Result<RentGoodsDetailVo> detail(@PathVariable("id") Long id) {
        RentGoodsDetailVo detailVo = rentGoodsService.detail(id);
        if (detailVo.getStatus() == null || detailVo.getStatus() != 1) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }
        merchantStoreOpenSupport.assertMerchantStoreOpen(detailVo.getMerchantId());
        return Result.success(detailVo);
    }
}
