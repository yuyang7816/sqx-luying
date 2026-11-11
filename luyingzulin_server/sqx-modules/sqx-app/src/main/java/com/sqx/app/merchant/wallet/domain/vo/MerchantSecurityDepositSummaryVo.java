package com.sqx.app.merchant.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商户保证金概览")
public class MerchantSecurityDepositSummaryVo {

    @Schema(description = "保证金总额")
    private BigDecimal totalAmount;

    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;

    @Schema(description = "可用保证金")
    private BigDecimal availableAmount;

    @Schema(description = "状态 1正常 2冻结")
    private Integer status;

    @Schema(description = "系统配置应缴保证金")
    private BigDecimal requiredAmount;

    @Schema(description = "待缴金额")
    private BigDecimal needPayAmount;

    @Schema(description = "是否需缴纳保证金（配置金额>0）")
    private Boolean depositRequired;

    @Schema(description = "是否已缴足保证金")
    private Boolean depositPaid;

    @Schema(description = "是否可申请退还")
    private Boolean canRefund;

    @Schema(description = "是否有退保申请审核中")
    private Boolean refundPending;

    @Schema(description = "审核中退保金额")
    private BigDecimal pendingRefundAmount;
}
