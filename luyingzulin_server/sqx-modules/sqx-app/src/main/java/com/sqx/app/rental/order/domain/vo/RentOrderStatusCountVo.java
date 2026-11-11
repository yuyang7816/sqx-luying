package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 我的页订单角标统计
 */
@Data
@Schema(description = "订单状态角标统计")
public class RentOrderStatusCountVo {

    @Schema(description = "待支付")
    @JsonProperty("pending_payment")
    private Integer pendingPayment;

    @Schema(description = "待取货（含配送中）")
    @JsonProperty("waiting_pickup")
    private Integer waitingPickup;

    @Schema(description = "租赁中")
    private Integer renting;

    @Schema(description = "待归还（含验收中）")
    private Integer returning;

    @Schema(description = "售后（退款中）")
    @JsonProperty("after_sale")
    private Integer afterSale;
}
