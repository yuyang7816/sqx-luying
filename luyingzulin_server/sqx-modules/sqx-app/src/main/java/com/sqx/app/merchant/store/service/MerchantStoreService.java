package com.sqx.app.merchant.store.service;

import com.sqx.app.merchant.store.domain.bo.MerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.vo.MerchantStoreDetailVo;

public interface MerchantStoreService {

    MerchantStoreDetailVo detail(Long userId);

    void save(Long userId, MerchantStoreSaveBo saveBo);
}
