package com.sqx.app.rental.dashboard.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "看板趋势项")
public class AdminRentDashboardTrendItemVo {

    @Schema(description = "日期 MM-dd")
    private String date;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "成交额")
    private BigDecimal payAmount;
}
