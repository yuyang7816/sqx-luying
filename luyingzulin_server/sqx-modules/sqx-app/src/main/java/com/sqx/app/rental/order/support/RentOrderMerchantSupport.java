package com.sqx.app.rental.order.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 订单商户解析：以订单商品绑定的商户为准。
 */
@Component
public class RentOrderMerchantSupport {

    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    /**
     * 从订单商品解析商户 ID，商品绑定优先于订单冗余字段。
     */
    public Long resolveMerchantId(Long orderId, Long fallbackMerchantId) {
        if (orderId == null) {
            return fallbackMerchantId;
        }
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, orderId)
                .orderByAsc(RentOrderItem::getId));
        for (RentOrderItem item : items) {
            if (item.getGoodsId() == null) {
                continue;
            }
            RentGoods goods = rentGoodsMapper.selectById(item.getGoodsId());
            if (goods != null && goods.getMerchantId() != null) {
                return goods.getMerchantId();
            }
        }
        return fallbackMerchantId;
    }

    public Merchant resolveMerchant(Long orderId, Long fallbackMerchantId) {
        Long merchantId = resolveMerchantId(orderId, fallbackMerchantId);
        if (merchantId == null) {
            return null;
        }
        return merchantMapper.selectById(merchantId);
    }

    /**
     * 解析展示用门店：优先订单所选门店（且须属于商品商户），否则取该商户首个启用门店。
     */
    public MerchantStore resolveMerchantStore(Long merchantId, Long preferredStoreId) {
        if (preferredStoreId != null) {
            MerchantStore preferred = merchantStoreMapper.selectById(preferredStoreId);
            if (preferred != null && (merchantId == null || merchantId.equals(preferred.getMerchantId()))) {
                return preferred;
            }
        }
        if (merchantId == null) {
            return preferredStoreId == null ? null : merchantStoreMapper.selectById(preferredStoreId);
        }
        return merchantStoreMapper.selectOne(new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getMerchantId, merchantId)
                .eq(MerchantStore::getStatus, 1)
                .orderByAsc(MerchantStore::getId)
                .last("LIMIT 1"));
    }
}
