package com.sqx.app.rental.order.support;

import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.redis.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 同城配送费计算：
 * 1. 仅 pickupType=delivery 时收取；
 * 2. 优先使用门店 baseDeliveryFee，未配置则回落平台默认 rental.delivery.base_fee；
 * 3. 租金 rentAmount 达到门店 freeDeliveryThreshold 时免配送费。
 */
@Component
public class RentalDeliveryFeeSupport {

    public static final String PLATFORM_FEE_KEY = "rental.delivery.base_fee";
    private static final BigDecimal FALLBACK_FEE = new BigDecimal("15.00");

    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    public BigDecimal calc(String pickupType, Long pickupStoreId, BigDecimal rentAmount) {
        if (!PickupType.DELIVERY.getCode().equals(pickupType)) {
            return zero();
        }
        MerchantStore store = pickupStoreId == null ? null : merchantStoreMapper.selectById(pickupStoreId);
        if (store != null && store.getDeliveryEnabled() != null && store.getDeliveryEnabled() == 0) {
            throw new BizException("该门店暂不支持同城配送");
        }
        if (store != null && isFreeDelivery(store.getFreeDeliveryThreshold(), rentAmount)) {
            return zero();
        }
        BigDecimal baseFee = resolveBaseFee(store);
        return baseFee.setScale(2, RoundingMode.HALF_UP);
    }

    public String buildRuleRemark(String pickupType, Long pickupStoreId, BigDecimal rentAmount) {
        if (!PickupType.DELIVERY.getCode().equals(pickupType)) {
            return "";
        }
        MerchantStore store = pickupStoreId == null ? null : merchantStoreMapper.selectById(pickupStoreId);
        BigDecimal fee = calc(pickupType, pickupStoreId, rentAmount);
        BigDecimal baseFee = resolveBaseFee(store);
        StringBuilder sb = new StringBuilder("同城配送费按取货门店规则计算");
        if (store != null && StringUtils.isNotBlank(store.getName())) {
            sb.append("（").append(store.getName()).append("）");
        }
        sb.append("，基础配送费 ¥").append(baseFee.toPlainString());
        if (store != null && store.getFreeDeliveryThreshold() != null
                && store.getFreeDeliveryThreshold().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("；租金满 ¥").append(store.getFreeDeliveryThreshold().stripTrailingZeros().toPlainString())
                    .append(" 免配送（不含押金）");
        }
        if (fee.compareTo(BigDecimal.ZERO) == 0 && baseFee.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("。当前订单已满足免配送条件");
        } else if (fee.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("。本单配送费 ¥").append(fee.toPlainString());
        }
        return sb.toString();
    }

    private BigDecimal resolveBaseFee(MerchantStore store) {
        if (store != null && store.getBaseDeliveryFee() != null
                && store.getBaseDeliveryFee().compareTo(BigDecimal.ZERO) >= 0) {
            return store.getBaseDeliveryFee();
        }
        return resolvePlatformDefaultFee();
    }

    private BigDecimal resolvePlatformDefaultFee() {
        try {
            String value = ConfigUtil.getConfigValueOrDefault(PLATFORM_FEE_KEY);
            if (StringUtils.isNotBlank(value)) {
                return new BigDecimal(value.trim());
            }
        } catch (Exception ignored) {
            // 配置缺失时使用兜底值
        }
        return FALLBACK_FEE;
    }

    private boolean isFreeDelivery(BigDecimal threshold, BigDecimal rentAmount) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) <= 0 || rentAmount == null) {
            return false;
        }
        return rentAmount.compareTo(threshold) >= 0;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
