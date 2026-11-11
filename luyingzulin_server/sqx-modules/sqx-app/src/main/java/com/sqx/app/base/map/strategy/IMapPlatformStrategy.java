package com.sqx.app.base.map.strategy;

import com.sqx.app.base.map.domain.vo.AddressVo;

/**
 * @author chengming
 * @since 2025/8/11
 */
public interface IMapPlatformStrategy {

    /**
     * 反向地理编码（根据经纬度获取地址信息）
     * @param lat 维度
     * @param lng 经度
     * @param coordType 坐标类型（平台相关，可为空）
     */
    AddressVo reverseGeocode(String lat, String lng, String coordType);
}
