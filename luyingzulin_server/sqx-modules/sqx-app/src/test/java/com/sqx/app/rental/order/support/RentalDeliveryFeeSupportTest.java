package com.sqx.app.rental.order.support;

import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.enums.PickupType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalDeliveryFeeSupportTest {

    @InjectMocks
    private RentalDeliveryFeeSupport rentalDeliveryFeeSupport;

    @Mock
    private MerchantStoreMapper merchantStoreMapper;

    @Test
    void calcShouldReturnZeroForStorePickup() {
        assertEquals(new BigDecimal("0.00"),
                rentalDeliveryFeeSupport.calc(PickupType.STORE.getCode(), 1L, new BigDecimal("100")));
    }

    @Test
    void calcShouldUseStoreBaseFeeForDelivery() {
        MerchantStore store = new MerchantStore();
        store.setBaseDeliveryFee(new BigDecimal("12.00"));
        store.setDeliveryEnabled(1);
        when(merchantStoreMapper.selectById(1L)).thenReturn(store);

        assertEquals(new BigDecimal("12.00"),
                rentalDeliveryFeeSupport.calc(PickupType.DELIVERY.getCode(), 1L, new BigDecimal("50")));
    }

    @Test
    void calcShouldWaiveDeliveryWhenRentMeetsThreshold() {
        MerchantStore store = new MerchantStore();
        store.setBaseDeliveryFee(new BigDecimal("12.00"));
        store.setFreeDeliveryThreshold(new BigDecimal("100.00"));
        store.setDeliveryEnabled(1);
        when(merchantStoreMapper.selectById(1L)).thenReturn(store);

        assertEquals(new BigDecimal("0.00"),
                rentalDeliveryFeeSupport.calc(PickupType.DELIVERY.getCode(), 1L, new BigDecimal("120")));
    }
}
