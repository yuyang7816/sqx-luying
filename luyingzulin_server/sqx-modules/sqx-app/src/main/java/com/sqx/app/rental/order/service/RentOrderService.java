package com.sqx.app.rental.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.order.domain.bo.AppRentOrderPageBo;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateBo;
import com.sqx.app.rental.order.domain.bo.RentOrderPageBo;
import com.sqx.app.rental.order.domain.vo.AppRentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderCreateVo;
import com.sqx.app.rental.order.domain.vo.RentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderListVo;
import com.sqx.app.rental.order.domain.vo.RentOrderPayStatusVo;
import com.sqx.app.rental.order.domain.vo.RentOrderStatusCountVo;

import java.math.BigDecimal;

/**
 * 租赁订单服务
 */
public interface RentOrderService {

    /**
     * 统计当前用户各状态订单数量（我的页角标）
     */
    RentOrderStatusCountVo statusCounts(Long userId);

    /**
     * 创建订单（pending_payment + 库存 PRELOCK）
     */
    RentOrderCreateVo createOrder(Long userId, RentOrderCreateBo createBo);

    /**
     * 取消待支付订单并释放预占
     */
    void cancelOrder(Long userId, Long orderId);

    /**
     * 支付成功回调（幂等）
     */
    void handlePaySuccess(String orderNo, String payChannel, String payNo, BigDecimal paidAmount);

    /**
     * 支付前校验并返回应付金额
     */
    BigDecimal prepareRentOrderPay(Long userId, String orderNo, BigDecimal requestAmount);

    /**
     * 查询支付状态
     */
    RentOrderPayStatusVo payStatus(Long userId, String orderNo);

    /**
     * 管理端订单分页
     */
    IPage<RentOrderListVo> adminPage(RentOrderPageBo pageBo);

    /**
     * 管理端订单详情
     */
    RentOrderDetailVo adminDetail(Long orderId);

    /**
     * 管理端代取消待支付订单
     */
    void adminCancelOrder(Long orderId);

    /**
     * 用户端订单分页
     */
    IPage<RentOrderListVo> appPage(Long userId, AppRentOrderPageBo pageBo);

    /**
     * 用户端订单详情
     */
    AppRentOrderDetailVo appDetail(Long userId, Long orderId);
}
