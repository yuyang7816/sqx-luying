package com.sqx.app.vip.order.service;

import com.sqx.app.vip.order.domain.vo.VipPayStatusVo;
import com.sqx.app.vip.order.domain.vo.VipPurchaseVo;

import java.math.BigDecimal;

public interface VipOrderService {

    VipPurchaseVo createPurchase(Long userId, Long planId);

    BigDecimal preparePay(Long userId, String orderNo, BigDecimal amount);

    void handlePaySuccess(String orderNo, String payChannel, String payTradeNo, BigDecimal amount);

    VipPayStatusVo payStatus(Long userId, String orderNo);
}
