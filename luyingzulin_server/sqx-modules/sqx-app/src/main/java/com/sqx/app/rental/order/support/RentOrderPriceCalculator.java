package com.sqx.app.rental.order.support;

import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.enums.RentType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import com.sqx.common.core.exception.BizException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 租赁订单金额计算
 */
public final class RentOrderPriceCalculator {

    /** 同城配送费（元） */
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("15.00");

    private RentOrderPriceCalculator() {
    }

    public static int resolveDays(LocalDateTime start, LocalDateTime end, Integer days) {
        if (days != null && days > 0) {
            return days;
        }
        long between = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        return (int) Math.max(between, 1);
    }

    public static BigDecimal unitRentPrice(RentGoods goods, String rentType) {
        RentType type = RentType.fromCode(rentType);
        switch (type) {
            case DAY:
                return defaultZero(goods.getRentPriceDay());
            case HOUR:
                if (goods.getRentPriceHour() == null) {
                    throw new BizException("商品不支持时租");
                }
                return goods.getRentPriceHour();
            case OVERNIGHT:
                if (goods.getRentPriceOvernight() == null) {
                    throw new BizException("商品不支持过夜租");
                }
                return goods.getRentPriceOvernight();
            default:
                throw new BizException("不支持的租赁类型");
        }
    }

    public static BigDecimal calcItemRentAmount(RentGoods goods, String rentType, LocalDateTime start,
                                              LocalDateTime end, Integer days, int qty,
                                              RentGoodsPackage goodsPackage) {
        return calcItemRentAmount(goods, rentType, start, end, days, qty, goodsPackage, null);
    }

    public static BigDecimal calcItemRentAmount(RentGoods goods, String rentType, LocalDateTime start,
                                              LocalDateTime end, Integer days, int qty,
                                              RentGoodsPackage goodsPackage, RentGoodsSpec goodsSpec) {
        BigDecimal unitPrice = unitRentPrice(goods, rentType);
        BigDecimal rentAmount;
        RentType type = RentType.fromCode(rentType);
        if (RentType.DAY == type) {
            int rentDays = resolveDays(start, end, days);
            rentAmount = unitPrice.multiply(BigDecimal.valueOf(rentDays));
        } else if (RentType.HOUR == type) {
            long hours = Math.max(Duration.between(start, end).toHours(), 1);
            rentAmount = unitPrice.multiply(BigDecimal.valueOf(hours));
        } else {
            rentAmount = unitPrice;
        }
        BigDecimal specExtra = resolveSpecExtraPrice(goodsSpec);
        if (specExtra.compareTo(BigDecimal.ZERO) > 0) {
            rentAmount = rentAmount.add(specExtra);
        }
        if (goodsPackage != null && goodsPackage.getPrice() != null) {
            rentAmount = rentAmount.add(goodsPackage.getPrice());
        }
        return rentAmount.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal resolveSpecExtraPrice(RentGoodsSpec goodsSpec) {
        if (goodsSpec == null || goodsSpec.getSpecValue() == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(goodsSpec.getSpecValue().trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal calcItemDeposit(RentGoods goods, int qty) {
        return defaultZero(goods.getDepositAmount()).multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calcDeliveryAmount(String pickupType) {
        if (PickupType.DELIVERY.getCode().equals(pickupType)) {
            return DELIVERY_FEE;
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 续租计费小时数（不足 1 小时按 1 小时计，向上取整）
     */
    public static long resolveRenewBillingHours(LocalDateTime extendStart, LocalDateTime newEndTime) {
        if (extendStart == null || newEndTime == null || !newEndTime.isAfter(extendStart)) {
            throw new BizException("续租结束时间须晚于当前租期结束时间");
        }
        long minutes = Duration.between(extendStart, newEndTime).toMinutes();
        return Math.max((minutes + 59) / 60, 1);
    }

    /**
     * 续租时租单价（优先商品时租价，否则按日租价 / 24 折算）
     */
    public static BigDecimal resolveHourlyUnitPrice(RentGoods goods) {
        if (goods.getRentPriceHour() != null && goods.getRentPriceHour().compareTo(BigDecimal.ZERO) > 0) {
            return goods.getRentPriceHour();
        }
        BigDecimal dayPrice = defaultZero(goods.getRentPriceDay());
        if (dayPrice.compareTo(BigDecimal.ZERO) > 0) {
            return dayPrice.divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP);
        }
        throw new BizException("商品未配置时租单价");
    }

    /**
     * 续租租金：续租时长（小时）× 时租单价 × 数量
     */
    public static BigDecimal calcRenewItemAmountByHourlyRate(RentGoods goods, LocalDateTime extendStart,
                                                           LocalDateTime newEndTime, int qty) {
        long hours = resolveRenewBillingHours(extendStart, newEndTime);
        BigDecimal hourPrice = resolveHourlyUnitPrice(goods);
        return hourPrice.multiply(BigDecimal.valueOf(hours))
                .multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 订单应付 = 租金 + 配送 - 优惠（押金从账户冻结，不计入支付金额）
     */
    public static BigDecimal calcPayAmount(BigDecimal rentAmount, BigDecimal depositAmount,
                                           BigDecimal deliveryAmount, BigDecimal couponAmount) {
        BigDecimal coupon = couponAmount == null ? BigDecimal.ZERO : couponAmount;
        return rentAmount.add(deliveryAmount).subtract(coupon)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static String generatePickupCode() {
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        return String.valueOf(code);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
