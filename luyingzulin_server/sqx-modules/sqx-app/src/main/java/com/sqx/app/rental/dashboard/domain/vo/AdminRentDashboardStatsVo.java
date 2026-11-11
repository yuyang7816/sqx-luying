package com.sqx.app.rental.dashboard.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端租赁看板统计
 */
@Data
@Schema(description = "租赁看板统计")
public class AdminRentDashboardStatsVo {

    @Schema(description = "分类总数")
    private Integer rentalCategoryTotal;

    @Schema(description = "商品总数")
    private Integer rentalGoodsTotal;

    @Schema(description = "今日订单数")
    private Integer todayOrderCount;

    @Schema(description = "今日支付金额")
    private BigDecimal todayPayAmount;

    @Schema(description = "租赁中订单数")
    private Integer rentingOrderCount;

    @Schema(description = "待处理数（归还中+验收中+待审核验收）")
    private Integer pendingCount;

    @Schema(description = "待归还订单数")
    private Integer pendingReturningCount;

    @Schema(description = "验收中订单数")
    private Integer pendingInspectingCount;

    @Schema(description = "待复核验收记录数")
    private Integer pendingInspectionCount;

    @Schema(description = "待处理用户反馈数")
    private Integer pendingFeedbackCount;

    @Schema(description = "待审核商户入驻申请数")
    private Integer pendingMerchantApplyCount;

    @Schema(description = "待处理提现数（发起提现）")
    private Integer pendingWithdrawCount;

    @Schema(description = "待审核用户退押申请数")
    private Integer pendingUserDepositRefundCount;

    @Schema(description = "C端用户总数")
    private Integer totalAppUserCount;

    @Schema(description = "已开通商户数")
    private Integer totalMerchantCount;

    @Schema(description = "累计分销佣金（已结算）")
    private BigDecimal totalReferralCommission;

    @Schema(description = "今日新增用户")
    private Integer todayNewUserCount;

    @Schema(description = "今日新增邀请注册")
    private Integer todayInviteRegisterCount;

    @Schema(description = "近7日订单趋势")
    private List<AdminRentDashboardTrendItemVo> orderTrend;

    @Schema(description = "订单状态分布")
    private List<AdminRentDashboardStatusItemVo> orderStatusDistribution;

    @Schema(description = "近7日成交额合计")
    private BigDecimal weekPayAmount;

    @Schema(description = "近7日订单数合计")
    private Integer weekOrderCount;
}
