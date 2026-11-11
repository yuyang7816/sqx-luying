package com.sqx.app.merchant.store.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStorePageBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreUpdateStatusBo;
import com.sqx.app.merchant.store.domain.vo.AdminMerchantStoreListVo;

public interface AdminMerchantStoreService {

    IPage<AdminMerchantStoreListVo> adminPage(AdminMerchantStorePageBo pageBo);

    void adminSave(AdminMerchantStoreSaveBo saveBo);

    void adminUpdateStatus(AdminMerchantStoreUpdateStatusBo updateStatusBo);
}
