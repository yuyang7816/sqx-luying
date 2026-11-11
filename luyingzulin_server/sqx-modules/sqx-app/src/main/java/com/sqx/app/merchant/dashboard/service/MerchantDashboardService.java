package com.sqx.app.merchant.dashboard.service;

import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardRevenueVo;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardStatsVo;

/**
 * 商户工作台统计
 */
public interface MerchantDashboardService {

    MerchantDashboardStatsVo stats(Long userId);

    MerchantDashboardRevenueVo revenue(Long userId, Integer year, Integer month);
}
