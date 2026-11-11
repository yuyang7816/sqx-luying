package com.sqx.app.rental.store.service;

import com.sqx.app.rental.store.domain.vo.RentPickupStoreVo;

import java.util.List;

public interface RentPickupStoreService {

    /**
     * 查询可用取货点位
     *
     * @param pickupType store / pickup_point，空则返回全部启用门店
     * @param merchantId 可选，仅返回指定商户门店
     */
    List<RentPickupStoreVo> list(String pickupType, Long merchantId);
}
