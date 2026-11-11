package com.sqx.app.vip.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "会员购买下单结果")
public class VipPurchaseVo {

    @Schema(description = "会员订单号")
    private String orderNo;

    @Schema(description = "套餐ID")
    private Long planId;

    @Schema(description = "套餐名称")
    private String planName;

    @Schema(description = "应付金额")
    private BigDecimal payAmount;
}
