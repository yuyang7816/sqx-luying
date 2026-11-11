package com.sqx.app.rental.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建续租结果
 */
@Data
@Schema(description = "创建续租结果")
public class RentRenewCreateVo {

    @Schema(description = "续租单号（支付 outTradeNo）")
    private String renewNo;

    @Schema(description = "原订单ID")
    private Long orderId;

    @Schema(description = "续租租金")
    private BigDecimal renewAmount;

    @Schema(description = "支付状态：pending")
    private String payStatus;
}
