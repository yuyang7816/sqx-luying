package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 租赁商品ID入参
 */
@Data
@Schema(description = "租赁商品ID入参")
public class RentGoodsInfoBo {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long id;
}
