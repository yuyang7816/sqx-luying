package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租赁商品更新入参
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "租赁商品更新入参")
public class RentGoodsUpdateBo extends RentGoodsSaveBo {

    @Schema(description = "商品ID，编辑时必填")
    private Long id;
}
