package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 租赁商品套餐项（保存/编辑）
 */
@Data
@Schema(description = "租赁商品套餐项")
public class RentGoodsPackageItemBo {

    @Schema(description = "套餐名称")
    @NotBlank(message = "套餐名称不能为空")
    private String packageName;

    @Schema(description = "套餐说明")
    private String packageDesc;

    @Schema(description = "套餐加价（元）")
    private BigDecimal price;

    @Schema(description = "排序")
    private Integer sort;
}
