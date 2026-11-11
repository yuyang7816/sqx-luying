package com.sqx.app.rental.user.support;

import com.sqx.app.rental.user.domain.entity.CouponTemplate;
import com.sqx.app.rental.user.enums.CouponType;
import com.sqx.common.core.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠券金额计算
 */
public final class CouponSupport {

    private CouponSupport() {
    }

    public static BigDecimal calcDiscount(CouponTemplate template, BigDecimal rentAmount,
                                          BigDecimal deliveryAmount, BigDecimal depositAmount) {
        if (template == null) {
            return BigDecimal.ZERO;
        }
        CouponType type = CouponType.fromCode(template.getCouponType());
        BigDecimal rent = nullToZero(rentAmount);
        BigDecimal delivery = nullToZero(deliveryAmount);
        BigDecimal deposit = nullToZero(depositAmount);
        BigDecimal minAmount = template.getMinAmount() == null ? BigDecimal.ZERO : template.getMinAmount();

        switch (type) {
            case AMOUNT_OFF:
                if (rent.compareTo(minAmount) < 0) {
                    throw new BizException("未达到优惠券使用门槛");
                }
                return template.getAmount().min(rent).max(BigDecimal.ZERO);
            case DISCOUNT:
                if (rent.compareTo(minAmount) < 0) {
                    throw new BizException("未达到优惠券使用门槛");
                }
                BigDecimal rate = template.getDiscountRate();
                if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
                    return BigDecimal.ZERO;
                }
                return rent.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            case DELIVERY_FREE:
                return delivery.max(BigDecimal.ZERO);
            case DEPOSIT_FREE:
                return deposit.max(BigDecimal.ZERO);
            default:
                return BigDecimal.ZERO;
        }
    }

    public static String buildConditionText(CouponTemplate template) {
        if (template == null) {
            return "";
        }
        CouponType type = CouponType.fromCode(template.getCouponType());
        BigDecimal min = template.getMinAmount() == null ? BigDecimal.ZERO : template.getMinAmount();
        switch (type) {
            case AMOUNT_OFF:
                return min.compareTo(BigDecimal.ZERO) > 0 ? "满 " + min.stripTrailingZeros().toPlainString() + " 可用" : "无门槛";
            case DISCOUNT:
                return min.compareTo(BigDecimal.ZERO) > 0 ? "满 " + min.stripTrailingZeros().toPlainString() + " 享折扣" : "折扣券";
            case DELIVERY_FREE:
                return "同城配送可用";
            case DEPOSIT_FREE:
                return "免押租赁可用";
            default:
                return "";
        }
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
