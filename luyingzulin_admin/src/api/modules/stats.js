import { getRentalDashboardStats } from '@/api/rental/dashboard'

/** 数据看板：仅调用租赁专用统计接口，避免触发无权限的 sys/* 接口 */
export async function getDashboardStats() {
  const rentalStats = await getRentalDashboardStats()
  return {
    rentalCategoryTotal: rentalStats.rentalCategoryTotal,
    rentalGoodsTotal: rentalStats.rentalGoodsTotal,
    todayOrderCount: rentalStats.todayOrderCount,
    todayPayAmount: rentalStats.todayPayAmount,
    rentingOrderCount: rentalStats.rentingOrderCount,
    pendingCount: rentalStats.pendingCount,
    pendingReturningCount: rentalStats.pendingReturningCount ?? 0,
    pendingInspectingCount: rentalStats.pendingInspectingCount ?? 0,
    pendingInspectionCount: rentalStats.pendingInspectionCount ?? 0,
    pendingFeedbackCount: rentalStats.pendingFeedbackCount ?? 0,
    pendingMerchantApplyCount: rentalStats.pendingMerchantApplyCount ?? 0,
    pendingWithdrawCount: rentalStats.pendingWithdrawCount ?? 0,
    totalMerchantCount: rentalStats.totalMerchantCount ?? 0,
    totalReferralCommission: rentalStats.totalReferralCommission ?? 0,
    todayNewUserCount: rentalStats.todayNewUserCount ?? 0,
    todayInviteRegisterCount: rentalStats.todayInviteRegisterCount ?? 0,
    totalAppUserCount: rentalStats.totalAppUserCount ?? 0,
    weekOrderCount: rentalStats.weekOrderCount ?? 0,
    weekPayAmount: rentalStats.weekPayAmount ?? 0,
    orderTrend: rentalStats.orderTrend ?? [],
    orderStatusDistribution: rentalStats.orderStatusDistribution ?? [],
  }
}
