package com.sqx.app.merchant.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 商户门店营业状态校验（C 端商品可见性、下单拦截）
 */
@Component
public class MerchantStoreOpenSupport {

    public static final int STORE_STATUS_OPEN = 1;

    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    public boolean isMerchantStoreOpen(Long merchantId) {
        if (merchantId == null) {
            return true;
        }
        Long count = merchantStoreMapper.selectCount(new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getMerchantId, merchantId)
                .eq(MerchantStore::getStatus, STORE_STATUS_OPEN));
        return count != null && count > 0;
    }

    public void assertMerchantStoreOpen(Long merchantId) {
        if (merchantId == null) {
            return;
        }
        if (!isMerchantStoreOpen(merchantId)) {
            throw new BizException("店铺休息中，暂不可租");
        }
    }

    public void applyOpenStoreGoodsFilter(LambdaQueryWrapper<RentGoods> wrapper) {
        wrapper.and(w -> w.isNull(RentGoods::getMerchantId)
                .or()
                .inSql(RentGoods::getMerchantId,
                        "SELECT DISTINCT merchant_id FROM merchant_store WHERE status = 1 AND deleted = 0"));
    }

    /**
     * C 端按城市筛选：仅展示该城市营业门店所属商户商品。
     * 匹配 merchant_store.city（「西安」可匹配「西安市」）；无门店城市时不展示任何商品。
     */
    public void applyCityGoodsFilter(LambdaQueryWrapper<RentGoods> wrapper, String city) {
        if (StringUtils.isBlank(city)) {
            return;
        }
        String token = escapeSqlLiteral(StringUtils.trim(city));
        String sql = "SELECT DISTINCT merchant_id FROM merchant_store WHERE status = 1 AND deleted = 0 "
                + "AND (city LIKE '" + token + "%' OR REPLACE(city, '市', '') = '" + token + "')";
        wrapper.inSql(RentGoods::getMerchantId, sql);
    }

    private String escapeSqlLiteral(String value) {
        return StringUtils.replace(value, "'", "''");
    }
}
