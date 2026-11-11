package com.sqx.app.rental.category.controller;

import com.sqx.app.rental.category.domain.bo.RentCategoryIdBo;
import com.sqx.app.rental.category.domain.bo.RentCategorySaveBo;
import com.sqx.app.rental.category.domain.vo.RentCategoryInfoVo;
import com.sqx.app.rental.category.domain.vo.RentCategoryTreeVo;
import com.sqx.app.rental.category.service.RentCategoryService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 管理端-租赁分类
 */
@Tag(name = "管理端-租赁分类", description = "租赁商品分类维护")
@RestController
@RequestMapping("/admin/rental/category")
public class AdminRentCategoryController {

    @Resource
    private RentCategoryService rentCategoryService;

    @Operation(summary = "分类树", description = "管理端查看分类树结构（含禁用项）")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentCategoryTreeVo.class)))
    @GetMapping("/tree")
    public Result<List<RentCategoryTreeVo>> tree() {
        return Result.success(rentCategoryService.adminTree());
    }

    @Operation(summary = "分类详情")
    @PostMapping("/info")
    public Result<RentCategoryInfoVo> info(@RequestBody @Validated RentCategoryIdBo idBo) {
        return Result.success(rentCategoryService.info(idBo.getId()));
    }

    @Operation(summary = "新增分类")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated RentCategorySaveBo saveBo) {
        rentCategoryService.save(saveBo);
        return Result.success();
    }

    @Operation(summary = "更新分类")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Validated RentCategorySaveBo saveBo) {
        rentCategoryService.update(saveBo);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody @Validated RentCategoryIdBo idBo) {
        rentCategoryService.delete(idBo.getId());
        return Result.success();
    }
}
