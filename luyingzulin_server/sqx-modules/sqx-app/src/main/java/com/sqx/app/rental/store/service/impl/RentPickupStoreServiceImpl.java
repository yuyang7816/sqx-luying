package com.sqx.app.rental.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.store.domain.vo.RentPickupStoreVo;
import com.sqx.app.rental.store.service.RentPickupStoreService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RentPickupStoreServiceImpl implements RentPickupStoreService {

    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    @Override
    public List<RentPickupStoreVo> list(String pickupType, Long merchantId) {
        LambdaQueryWrapper<MerchantStore> wrapper = new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getStatus, 1)
                .orderByAsc(MerchantStore::getId);
        if (merchantId != null) {
            wrapper.eq(MerchantStore::getMerchantId, merchantId);
        }
        if (PickupType.PICKUP_POINT.getCode().equals(pickupType)) {
            wrapper.and(w -> w.eq(MerchantStore::getSelfPickupEnabled, 1)
                    .or()
                    .isNull(MerchantStore::getSelfPickupEnabled));
        }
        List<MerchantStore> stores = merchantStoreMapper.selectList(wrapper);
        String resolvedType = StringUtils.hasText(pickupType) ? pickupType : PickupType.STORE.getCode();
        return stores.stream().map(store -> toVo(store, resolvedType)).collect(Collectors.toList());
    }

    private RentPickupStoreVo toVo(MerchantStore store, String pickupType) {
        RentPickupStoreVo vo = new RentPickupStoreVo();
        vo.setId(store.getId());
        vo.setMerchantId(store.getMerchantId());
        vo.setStoreNo(store.getStoreNo());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setContactPhone(store.getContactPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setLongitude(store.getLongitude());
        vo.setLatitude(store.getLatitude());
        vo.setBaseDeliveryFee(store.getBaseDeliveryFee());
        vo.setFreeDeliveryThreshold(store.getFreeDeliveryThreshold());
        vo.setPickupType(pickupType);
        return vo;
    }
}
