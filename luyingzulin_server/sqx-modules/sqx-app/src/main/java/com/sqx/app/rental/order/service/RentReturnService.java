package com.sqx.app.rental.order.service;

import com.sqx.app.rental.order.domain.bo.RentReturnApplyBo;
import com.sqx.app.rental.order.domain.vo.RentReturnDetailVo;

/**
 * 归还服务
 */
public interface RentReturnService {

    /**
     * 提交归还申请（renting → returning）
     */
    void applyReturn(Long userId, RentReturnApplyBo applyBo);

    /**
     * 归还详情
     */
    RentReturnDetailVo returnDetail(Long userId, Long orderId);
}
