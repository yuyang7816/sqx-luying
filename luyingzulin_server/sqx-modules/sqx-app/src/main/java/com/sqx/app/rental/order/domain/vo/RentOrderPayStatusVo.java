package com.sqx.app.rental.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付状态查询
 */
@Data
@Schema(description = "支付状态查询")
public class RentOrderPayStatusVo {

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "是否已支付")
    private Boolean paid;

    @Schema(description = "取货码")
    private String pickupCode;
}
