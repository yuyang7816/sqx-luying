package com.sqx.app.rental.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租赁商品规格
 */
@Data
@Schema(description = "租赁商品规格")
public class RentGoodsSpecVo {

    @Schema(description = "规格ID")
    private Long id;

    @Schema(description = "规格名称")
    private String name;

    @Schema(description = "规格值")
    private String value;

    @Schema(description = "规格价格（元）")
    private BigDecimal price;
}
