package com.sqx.app.merchant.apply.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.apply.domain.bo.MerchantPageBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantUpdateStatusBo;
import com.sqx.app.merchant.apply.domain.vo.MerchantDetailVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantListVo;

public interface MerchantAdminService {

    IPage<MerchantListVo> page(MerchantPageBo pageBo);

    MerchantDetailVo detail(Long id);

    void updateStatus(MerchantUpdateStatusBo updateStatusBo);
}
