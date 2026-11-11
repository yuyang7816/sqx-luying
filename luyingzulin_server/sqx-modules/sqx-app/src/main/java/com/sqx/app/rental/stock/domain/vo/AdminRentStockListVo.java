package com.sqx.app.rental.stock.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端库存列表项
 */
@Data
@Schema(description = "管理端库存列表项")
public class AdminRentStockListVo {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品编号")
    private String goodsNo;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "可用库存")
    private Integer availableStock;

    @Schema(description = "占用库存")
    private Integer occupiedStock;

    @Schema(description = "上下架状态")
    private Integer status;
}
