package com.sqx.app.rental.stock.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理端库存分页
 */
@Data
@Schema(description = "管理端库存分页")
public class AdminRentStockPageBo {

    @Schema(description = "页码")
    private Long page = 1L;

    @Schema(description = "每页条数")
    private Long limit = 20L;

    @Schema(description = "关键词（商品名/编号）")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;
}
