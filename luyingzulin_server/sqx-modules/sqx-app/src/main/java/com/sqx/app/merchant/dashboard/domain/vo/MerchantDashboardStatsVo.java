package com.sqx.app.merchant.dashboard.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商户工作台统计
 */
@Data
@Schema(description = "商户工作台统计")
public class MerchantDashboardStatsVo {

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "营业状态：1 营业中 0 休息")
    private Integer businessStatus;

    @Schema(description = "商品总数")
    private Integer goodsTotalCount;

    @Schema(description = "上架商品数")
    private Integer goodsOnShelfCount;

    @Schema(description = "订单总量")
    private Integer orderTotalCount;

    @Schema(description = "待核销（待取货）")
    private Integer pendingVerifyCount;

    @Schema(description = "租赁中订单数")
    private Integer rentingCount;

    @Schema(description = "已退款/取消订单数")
    private Integer refundedCount;

    @Schema(description = "今日成交额")
    private BigDecimal todayAmount;

    @Schema(description = "当月成交额")
    private BigDecimal monthAmount;

    @Schema(description = "累计成交额")
    private BigDecimal totalAmount;

    @Schema(description = "今日待取货订单")
    private java.util.List<MerchantDashboardTodayOrderVo> todayPickupOrders;

    @Schema(description = "待备货订单数")
    private Integer pendingCount;

    @Schema(description = "待验收订单数")
    private Integer reviewCount;

    @Schema(description = "在架商品数（兼容旧字段）")
    private Integer goodsCount;

    @Schema(description = "本周成交额（兼容旧字段）")
    private BigDecimal weekAmount;
}
