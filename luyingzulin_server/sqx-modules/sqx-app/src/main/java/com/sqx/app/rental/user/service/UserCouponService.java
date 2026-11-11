package com.sqx.app.rental.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.*;
import com.sqx.app.rental.user.domain.vo.ClaimableCouponVo;
import com.sqx.app.rental.user.domain.vo.UserCouponVo;

import java.math.BigDecimal;
import java.util.List;

public interface UserCouponService {

    IPage<UserCouponVo> page(Long userId, UserCouponPageBo pageBo);

    List<ClaimableCouponVo> listClaimable(Long userId);

    void claim(Long userId, Long templateId);

    List<UserCouponVo> listUsable(Long userId, UserCouponUsableBo usableBo);

    BigDecimal previewDiscount(Long userId, Long couponId, BigDecimal rentAmount,
                               BigDecimal deliveryAmount, BigDecimal depositAmount);

    void consume(Long userId, UserCouponConsumeBo consumeBo);

    void consumeForOrder(Long userId, Long couponId, String orderNo);

    void markExpiredCoupons(Long userId);
}
