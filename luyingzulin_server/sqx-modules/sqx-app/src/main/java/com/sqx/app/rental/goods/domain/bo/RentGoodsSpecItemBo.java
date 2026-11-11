package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 租赁商品规格项（保存/编辑）
 */
@Data
@Schema(description = "租赁商品规格项")
public class RentGoodsSpecItemBo {

    @Schema(description = "规格名称")
    @NotBlank(message = "规格名称不能为空")
    private String specName;

    @Schema(description = "规格值（兼容旧数据）")
    private String specValue;

    @Schema(description = "规格价格（元）")
    private BigDecimal specPrice;

    @Schema(description = "规格类型：1租赁规格 2商品参数")
    private Integer specType;

    @Schema(description = "排序")
    private Integer sort;
}
