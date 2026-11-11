package com.sqx.app.rental.goods.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.goods.domain.bo.RentGoodsInfoBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSaveBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateStateBo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.app.rental.goods.service.RentGoodsService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-租赁商品
 */
@Tag(name = "管理端-租赁商品", description = "租赁商品维护")
@RestController
@RequestMapping("/admin/rental/goods")
public class AdminRentGoodsController {

    @Resource
    private RentGoodsService rentGoodsService;

    @Operation(summary = "商品分页", description = "支持分类、关键词、状态筛选")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentGoodsListVo.class)))
    @PostMapping("/page")
    public Result<IPage<RentGoodsListVo>> page(@RequestBody @Validated RentGoodsPageBo pageBo) {
        return Result.success(rentGoodsService.page(pageBo));
    }

    @Operation(summary = "商品详情")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentGoodsDetailVo.class)))
    @GetMapping("/detail/{id}")
    public Result<RentGoodsDetailVo> detail(@PathVariable("id") Long id) {
        return Result.success(rentGoodsService.detail(id));
    }

    @Operation(summary = "新增商品", description = "含规格、套餐、图片、库存")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated RentGoodsSaveBo saveBo) {
        rentGoodsService.saveInfo(saveBo);
        return Result.success();
    }

    @Operation(summary = "更新商品", description = "含规格、套餐、图片、库存")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Validated RentGoodsUpdateBo updateBo) {
        rentGoodsService.updateInfo(updateBo);
        return Result.success();
    }

    @Operation(summary = "删除商品")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody @Validated RentGoodsInfoBo infoBo) {
        rentGoodsService.delete(infoBo);
        return Result.success();
    }

    @Operation(summary = "上下架")
    @PostMapping("/updateState")
    public Result<Void> updateState(@RequestBody @Validated RentGoodsUpdateStateBo updateStateBo) {
        rentGoodsService.updateState(updateStateBo);
        return Result.success();
    }
}
