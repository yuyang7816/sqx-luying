package com.sqx.app.merchant.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPageBo;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPickupBo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderContactVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderDetailVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderListVo;

public interface MerchantOrderService {

    IPage<MerchantOrderListVo> page(Long userId, MerchantOrderPageBo pageBo);

    MerchantOrderDetailVo detail(Long userId, String id);

    void prepare(Long userId, String id);

    void pickup(Long userId, String id, MerchantOrderPickupBo pickupBo);

    MerchantOrderDetailVo pickupByCode(Long userId, MerchantOrderPickupBo pickupBo);

    MerchantOrderContactVo contact(Long userId, String id);
}
