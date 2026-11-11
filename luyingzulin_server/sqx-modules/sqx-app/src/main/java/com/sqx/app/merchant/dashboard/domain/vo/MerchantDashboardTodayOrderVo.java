package com.sqx.app.merchant.dashboard.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 今日待取货订单（工作台列表）
 */
@Data
@Schema(description = "今日待取货订单")
public class MerchantDashboardTodayOrderVo {

    @Schema(description = "商户订单ID")
    private Long id;

    @Schema(description = "商户订单号")
    private String merchantOrderNo;

    @Schema(description = "商品摘要")
    private String goodsName;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系人手机号")
    private String contactPhone;

    @Schema(description = "预约取货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupStartTime;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "可执行操作：prepare/pickup/none")
    private String nextAction;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;
}
