package com.sqx.app.rental.category.controller;

import com.sqx.app.rental.category.domain.vo.RentCategoryTreeVo;
import com.sqx.app.rental.category.service.RentCategoryService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户端-租赁分类
 */
@Tag(name = "用户端-租赁分类", description = "租赁商品分类树")
@RestController
@RequestMapping("/app/rental/category")
public class AppRentCategoryController {

    @Resource
    private RentCategoryService rentCategoryService;

    @Operation(summary = "分类树", description = "返回启用状态的分类树，含子分类")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentCategoryTreeVo.class)))
    @GetMapping("/tree")
    public Result<List<RentCategoryTreeVo>> tree() {
        return Result.success(rentCategoryService.tree());
    }
}
