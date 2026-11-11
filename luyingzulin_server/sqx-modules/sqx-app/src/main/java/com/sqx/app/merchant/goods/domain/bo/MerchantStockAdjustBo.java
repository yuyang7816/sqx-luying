package com.sqx.app.merchant.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 商户库存调整入参
 */
@Data
@Schema(description = "商户库存调整入参")
public class MerchantStockAdjustBo {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @Schema(description = "规格ID，默认0")
    private Long specId;

    @Schema(description = "调整类型：in入库 out出库")
    @NotBlank(message = "调整类型不能为空")
    private String adjustType;

    @Schema(description = "调整数量（正整数）")
    @NotNull(message = "调整数量不能为空")
    @Min(value = 1, message = "调整数量至少为1")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;
}
