package com.sqx.app.merchant.order.support;

import com.sqx.app.merchant.enums.MerchantOrderStatus;
import com.sqx.app.merchant.order.domain.entity.MerchantOrder;
import com.sqx.app.merchant.order.mapper.MerchantOrderMapper;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantOrderSyncSupportTest {

    @InjectMocks
    private MerchantOrderSyncSupport syncSupport;

    @Mock
    private MerchantOrderMapper merchantOrderMapper;
    @Mock
    private RentOrderItemMapper rentOrderItemMapper;
    @Mock
    private MerchantStoreMapper merchantStoreMapper;

    @Test
    void syncOnPaySuccessShouldCreateLinkOrder() {
        RentOrder order = new RentOrder();
        order.setId(100L);
        order.setOrderNo("RO20260804001");
        order.setMerchantId(1L);
        order.setUserId(2L);
        order.setContactName("张三");
        order.setContactPhone("13800000000");
        order.setPickupType("store");
        order.setPayAmount(new java.math.BigDecimal("325.80"));

        when(merchantOrderMapper.selectOne(any())).thenReturn(null);
        when(rentOrderItemMapper.selectList(any())).thenReturn(java.util.Collections.singletonList(item("露营帐篷")));

        syncSupport.syncOnPaySuccess(order);

        ArgumentCaptor<MerchantOrder> captor = ArgumentCaptor.forClass(MerchantOrder.class);
        verify(merchantOrderMapper).insert(captor.capture());
        MerchantOrder saved = captor.getValue();
        assertEquals("LINK-100", saved.getMerchantOrderNo());
        assertEquals(MerchantOrderStatus.PENDING_PREPARE.getCode(), saved.getStatus());
        assertEquals(100L, saved.getRentOrderId());
        assertEquals("露营帐篷", saved.getGoodsName());
    }

    @Test
    void buildLinkNoFormat() {
        assertTrue(MerchantOrder.buildLinkNo(42L).startsWith("LINK-"));
        assertEquals("LINK-42", MerchantOrder.buildLinkNo(42L));
    }

    private RentOrderItem item(String name) {
        RentOrderItem item = new RentOrderItem();
        item.setGoodsName(name);
        return item;
    }
}
