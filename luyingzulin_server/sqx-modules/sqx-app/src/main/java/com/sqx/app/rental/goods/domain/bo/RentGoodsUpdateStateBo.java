package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 租赁商品上下架入参
 */
@Data
@Schema(description = "租赁商品上下架入参")
public class RentGoodsUpdateStateBo {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long id;

    @Schema(description = "状态：1上架 0下架")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
