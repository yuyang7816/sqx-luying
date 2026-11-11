package com.sqx.app.rental.order.support;

import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderLog;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.common.core.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 订单公共辅助
 */
public final class RentOrderSupport {

    private RentOrderSupport() {
    }

    public static RentOrder requireOwnedOrder(RentOrderMapper mapper, Long userId, Long orderId) {
        RentOrder order = mapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getId, orderId)
                .eq(RentOrder::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    public static RentOrder requireOrder(RentOrderMapper mapper, Long orderId) {
        RentOrder order = mapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    public static void writeOrderLog(RentOrderLogMapper logMapper, Long orderId, String orderNo,
                                     String fromStatus, String toStatus, String operatorType,
                                     Long operatorId, String remark) {
        RentOrderLog log = new RentOrderLog();
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        logMapper.insert(log);
    }

    public static String formatPickupCodeDisplay(String pickupCode) {
        if (pickupCode == null || pickupCode.length() < 4) {
            return pickupCode;
        }
        if (pickupCode.length() <= 4) {
            return pickupCode;
        }
        int mid = pickupCode.length() / 2;
        return pickupCode.substring(0, mid) + " " + pickupCode.substring(mid);
    }

    /** 核销比对用：去除空格等展示字符 */
    public static String normalizePickupCode(String pickupCode) {
        if (pickupCode == null) {
            return "";
        }
        return pickupCode.replaceAll("\\s+", "").trim();
    }
}
