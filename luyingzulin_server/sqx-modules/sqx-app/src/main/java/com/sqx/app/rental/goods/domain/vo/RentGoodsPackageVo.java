package com.sqx.app.rental.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 租赁商品配件套餐
 */
@Data
@Schema(description = "租赁商品配件套餐")
public class RentGoodsPackageVo {

    @Schema(description = "套餐ID")
    private Long id;

    @Schema(description = "套餐名称")
    private String packageName;

    @Schema(description = "套餐说明")
    private String packageDesc;

    @Schema(description = "套餐加价（元）")
    private BigDecimal price;
}
