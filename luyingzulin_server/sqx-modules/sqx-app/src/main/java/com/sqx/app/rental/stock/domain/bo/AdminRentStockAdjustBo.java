package com.sqx.app.rental.stock.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理端库存调整
 */
@Data
@Schema(description = "管理端库存调整")
public class AdminRentStockAdjustBo {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "规格ID，默认0")
    private Long specId;

    @NotNull(message = "调整数量不能为空")
    @Schema(description = "调整数量，正数增加负数减少")
    private Integer adjustQty;

    @Schema(description = "调整原因")
    private String reason;
}
