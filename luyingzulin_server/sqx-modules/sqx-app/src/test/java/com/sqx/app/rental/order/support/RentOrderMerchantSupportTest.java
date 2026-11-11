package com.sqx.app.rental.order.support;

import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentOrderMerchantSupportTest {

    @Mock
    private RentOrderItemMapper rentOrderItemMapper;
    @Mock
    private RentGoodsMapper rentGoodsMapper;
    @Mock
    private MerchantStoreMapper merchantStoreMapper;

    @InjectMocks
    private RentOrderMerchantSupport rentOrderMerchantSupport;

    @Test
    void resolveMerchantIdShouldPreferGoodsBinding() {
        RentOrderItem item = new RentOrderItem();
        item.setGoodsId(9L);
        when(rentOrderItemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        RentGoods goods = new RentGoods();
        goods.setId(9L);
        goods.setMerchantId(15L);
        when(rentGoodsMapper.selectById(9L)).thenReturn(goods);

        assertEquals(15L, rentOrderMerchantSupport.resolveMerchantId(45L, 1L));
    }

    @Test
    void resolveMerchantStoreShouldIgnoreMismatchedPreferredStore() {
        MerchantStore wrongStore = new MerchantStore();
        wrongStore.setId(1L);
        wrongStore.setMerchantId(99L);
        when(merchantStoreMapper.selectById(1L)).thenReturn(wrongStore);

        MerchantStore merchantStore = new MerchantStore();
        merchantStore.setId(2L);
        merchantStore.setMerchantId(15L);
        when(merchantStoreMapper.selectOne(any())).thenReturn(merchantStore);

        MerchantStore resolved = rentOrderMerchantSupport.resolveMerchantStore(15L, 1L);
        assertEquals(2L, resolved.getId());
    }
}
