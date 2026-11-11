package com.sqx.app.rental.order.service;

import com.sqx.app.rental.order.domain.vo.RentPickupCodeVo;

/**
 * 取货服务
 */
public interface RentPickupService {

    /**
     * 获取取货凭证
     */
    RentPickupCodeVo getPickupCode(Long userId, Long orderId);

    /**
     * 用户确认取货（waiting_pickup/delivering → renting）
     */
    void confirmPickup(Long userId, Long orderId);
}
