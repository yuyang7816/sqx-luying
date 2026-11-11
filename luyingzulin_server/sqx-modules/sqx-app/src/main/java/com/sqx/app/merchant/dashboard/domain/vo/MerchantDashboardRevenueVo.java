package com.sqx.app.merchant.dashboard.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户营业额筛选结果
 */
@Data
@Schema(description = "商户营业额筛选")
public class MerchantDashboardRevenueVo {

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份，0 表示全年")
    private Integer month;

    @Schema(description = "筛选期间成交额")
    private BigDecimal amount;

    @Schema(description = "筛选期间成交订单数")
    private Integer orderCount;

    @Schema(description = "今日成交额")
    private BigDecimal todayAmount;

    @Schema(description = "当月成交额")
    private BigDecimal monthAmount;

    @Schema(description = "累计成交额")
    private BigDecimal totalAmount;
}
