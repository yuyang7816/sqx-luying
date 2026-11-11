package com.sqx.app.rental.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建订单结果
 */
@Data
@Schema(description = "创建订单结果")
public class RentOrderCreateVo {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "应付总额")
    private BigDecimal payAmount;

    @Schema(description = "押金合计")
    private BigDecimal depositAmount;

    @Schema(description = "订单状态")
    private String status;
}
