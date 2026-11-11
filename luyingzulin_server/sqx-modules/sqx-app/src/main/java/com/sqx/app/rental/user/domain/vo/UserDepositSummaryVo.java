package com.sqx.app.rental.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserDepositSummaryVo {

    @Schema(description = "累计缴纳押金")
    private BigDecimal totalAmount;

    @Schema(description = "冻结中押金")
    private BigDecimal frozenAmount;

    @Schema(description = "可用押金")
    private BigDecimal availableAmount;

    @Schema(description = "状态：1正常 0冻结")
    private Integer status;

    @Schema(description = "审核中的退押金额")
    private BigDecimal pendingRefundAmount;

    @Schema(description = "是否有退押申请审核中")
    private Boolean refundPending;

    @Schema(description = "是否可申请退押")
    private Boolean canRefund;

    @Schema(description = "账户需保留的最低可用押金（0 表示不限制）")
    private BigDecimal minDepositBalance;
}
