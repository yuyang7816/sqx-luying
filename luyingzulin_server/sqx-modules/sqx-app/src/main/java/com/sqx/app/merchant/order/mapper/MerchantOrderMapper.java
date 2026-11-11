package com.sqx.app.merchant.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.merchant.order.domain.entity.MerchantOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantOrderMapper extends BaseMapper<MerchantOrder> {
}
