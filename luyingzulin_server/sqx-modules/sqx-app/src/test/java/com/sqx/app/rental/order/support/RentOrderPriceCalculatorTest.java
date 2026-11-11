package com.sqx.app.rental.order.support;

import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M03：订单金额计算单元测试
 */
class RentOrderPriceCalculatorTest {

    @Test
    void calcPayAmountShouldExcludeDepositAndSubtractCoupon() {
        BigDecimal pay = RentOrderPriceCalculator.calcPayAmount(
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("15.00"),
                new BigDecimal("10.00"));
        assertEquals(new BigDecimal("105.00"), pay);
    }

    @Test
    void resolveDaysShouldDefaultToAtLeastOne() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 12, 18, 0);
        assertEquals(2, RentOrderPriceCalculator.resolveDays(start, end, null));
        assertEquals(3, RentOrderPriceCalculator.resolveDays(start, end, 3));
    }

    @Test
    void generatePickupCodeShouldBeSixDigits() {
        String code = RentOrderPriceCalculator.generatePickupCode();
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void calcItemRentAmountShouldMultiplyDayPriceByDaysAndQty() {
        RentGoods goods = sampleGoods();
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 3, 18, 0);
        BigDecimal rent = RentOrderPriceCalculator.calcItemRentAmount(
                goods, "day", start, end, null, 2, null);
        assertEquals(new BigDecimal("200.00"), rent);
    }

    @Test
    void calcItemRentAmountShouldAddPackagePrice() {
        RentGoods goods = sampleGoods();
        RentGoodsPackage goodsPackage = new RentGoodsPackage();
        goodsPackage.setPrice(new BigDecimal("20.00"));
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 2, 18, 0);
        BigDecimal rent = RentOrderPriceCalculator.calcItemRentAmount(
                goods, "day", start, end, null, 1, goodsPackage);
        assertEquals(new BigDecimal("70.00"), rent);
    }

    @Test
    void calcItemRentAmountShouldAddSpecExtraPrice() {
        RentGoods goods = sampleGoods();
        RentGoodsSpec spec = new RentGoodsSpec();
        spec.setSpecName("4人帐");
        spec.setSpecValue("30");
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 2, 18, 0);
        BigDecimal rent = RentOrderPriceCalculator.calcItemRentAmount(
                goods, "day", start, end, null, 1, null, spec);
        assertEquals(new BigDecimal("80.00"), rent);
    }

    @Test
    void calcItemDepositShouldMultiplyByQty() {
        RentGoods goods = sampleGoods();
        assertEquals(new BigDecimal("400.00"), RentOrderPriceCalculator.calcItemDeposit(goods, 2));
    }

    @Test
    void calcDeliveryAmountShouldChargeOnlyForDelivery() {
        assertEquals(new BigDecimal("15.00"),
                RentOrderPriceCalculator.calcDeliveryAmount(PickupType.DELIVERY.getCode()));
        assertEquals(new BigDecimal("0.00"),
                RentOrderPriceCalculator.calcDeliveryAmount(PickupType.STORE.getCode()));
    }

    @Test
    void calcRenewItemAmountByHourlyRateShouldMultiplyHoursQtyAndHourPrice() {
        RentGoods goods = sampleGoods();
        goods.setRentPriceHour(new BigDecimal("10.00"));
        LocalDateTime extendStart = LocalDateTime.of(2026, 9, 3, 18, 0);
        LocalDateTime newEnd = extendStart.plusHours(5);
        BigDecimal amount = RentOrderPriceCalculator.calcRenewItemAmountByHourlyRate(
                goods, extendStart, newEnd, 2);
        assertEquals(new BigDecimal("100.00"), amount);
    }

    @Test
    void resolveHourlyUnitPriceShouldFallbackToDayPriceDividedBy24() {
        RentGoods goods = sampleGoods();
        assertEquals(new BigDecimal("2.08"), RentOrderPriceCalculator.resolveHourlyUnitPrice(goods));
    }

    private static RentGoods sampleGoods() {
        RentGoods goods = new RentGoods();
        goods.setRentPriceDay(new BigDecimal("50.00"));
        goods.setDepositAmount(new BigDecimal("200.00"));
        return goods;
    }
}
