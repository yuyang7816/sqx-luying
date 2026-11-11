package com.sqx.app.merchant.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户订单商品明细
 */
@Data
@Schema(description = "商户订单商品明细")
public class MerchantOrderGoodsItemVo {

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "规格描述")
    private String specInfo;

    @Schema(description = "配件套餐名称")
    private String packageName;

    @Schema(description = "配件套餐描述")
    private String packageDesc;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "租金单价")
    private BigDecimal rentPrice;
}
