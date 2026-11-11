package com.sqx.app.rental.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 租赁商品详情
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "租赁商品详情")
public class RentGoodsDetailVo extends RentGoodsListVo {

    @Schema(description = "商品详情描述")
    private String detail;

    @Schema(description = "轮播图列表")
    private List<String> images;

    @Schema(description = "租赁规格列表（用户可选）")
    private List<RentGoodsSpecVo> specs;

    @Schema(description = "商品参数列表（详情展示）")
    private List<RentGoodsParamVo> params;

    @Schema(description = "配件套餐列表")
    private List<RentGoodsPackageVo> packages;

    @Schema(description = "总库存")
    private Integer totalStock;
}
