package com.sqx.app.merchant.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantStoreOpenSupportTest {

    @Mock
    private MerchantStoreMapper merchantStoreMapper;

    @InjectMocks
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @Test
    void isMerchantStoreOpenShouldReturnTrueWhenMerchantIdIsNull() {
        assertTrue(merchantStoreOpenSupport.isMerchantStoreOpen(null));
    }

    @Test
    void isMerchantStoreOpenShouldReturnTrueWhenOpenStoreExists() {
        when(merchantStoreMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertTrue(merchantStoreOpenSupport.isMerchantStoreOpen(2L));
    }

    @Test
    void isMerchantStoreOpenShouldReturnFalseWhenNoOpenStore() {
        when(merchantStoreMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertFalse(merchantStoreOpenSupport.isMerchantStoreOpen(2L));
    }

    @Test
    void assertMerchantStoreOpenShouldThrowWhenStoreClosed() {
        when(merchantStoreMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BizException ex = assertThrows(BizException.class,
                () -> merchantStoreOpenSupport.assertMerchantStoreOpen(2L));

        assertTrue(ex.getMessage().contains("休息"));
    }

    @Test
    void assertMerchantStoreOpenShouldPassWhenStoreOpen() {
        when(merchantStoreMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertDoesNotThrow(() -> merchantStoreOpenSupport.assertMerchantStoreOpen(2L));
    }

    @Test
    void applyOpenStoreGoodsFilterShouldNotThrow() {
        LambdaQueryWrapper<RentGoods> wrapper = new LambdaQueryWrapper<>();

        assertDoesNotThrow(() -> merchantStoreOpenSupport.applyOpenStoreGoodsFilter(wrapper));
    }
}
