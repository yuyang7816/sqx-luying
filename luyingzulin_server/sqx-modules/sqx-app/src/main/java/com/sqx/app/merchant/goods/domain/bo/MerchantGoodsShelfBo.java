package com.sqx.app.merchant.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 商户商品上下架入参
 */
@Data
@Schema(description = "商户商品上下架入参")
public class MerchantGoodsShelfBo {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @Schema(description = "状态：1上架 0下架")
    @NotNull(message = "上下架状态不能为空")
    private Integer status;
}
