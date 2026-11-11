package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sqx.app.rental.user.domain.entity.CouponTemplate;
import com.sqx.app.rental.user.domain.entity.UserCoupon;
import com.sqx.app.rental.user.enums.CouponType;
import com.sqx.app.rental.user.enums.UserCouponStatus;
import com.sqx.app.rental.user.mapper.CouponTemplateMapper;
import com.sqx.app.rental.user.mapper.UserCouponMapper;
import com.sqx.app.rental.user.support.CouponSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * M06：优惠券计算、VO 组装与核销
 */
@ExtendWith(MockitoExtension.class)
class UserCouponServiceImplM06Test {

    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private CouponTemplateMapper couponTemplateMapper;
    @InjectMocks
    private UserCouponServiceImpl userCouponService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserCoupon.class);
    }

    @Test
    void calcDiscountShouldApplyAmountOffWhenReachThreshold() {
        CouponTemplate template = new CouponTemplate();
        template.setCouponType(CouponType.AMOUNT_OFF.getCode());
        template.setAmount(new BigDecimal("20.00"));
        template.setMinAmount(new BigDecimal("199.00"));

        BigDecimal discount = CouponSupport.calcDiscount(template,
                new BigDecimal("250.00"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(new BigDecimal("20.00"), discount);
    }

    @Test
    void calcDiscountShouldRejectWhenBelowThreshold() {
        CouponTemplate template = new CouponTemplate();
        template.setCouponType(CouponType.AMOUNT_OFF.getCode());
        template.setAmount(new BigDecimal("20.00"));
        template.setMinAmount(new BigDecimal("199.00"));

        assertThrows(BizException.class, () -> CouponSupport.calcDiscount(template,
                new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void toVoShouldIncludeTemplateBusinessFields() {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(1L);
        coupon.setCouponNo("CPN001");
        coupon.setStatus(UserCouponStatus.UNUSED.getCode());
        coupon.setExpireTime(LocalDateTime.now().plusDays(10));

        CouponTemplate template = new CouponTemplate();
        template.setTemplateNo("CTM001");
        template.setName("新人立减券");
        template.setCouponType(CouponType.AMOUNT_OFF.getCode());
        template.setAmount(new BigDecimal("20.00"));
        template.setMinAmount(new BigDecimal("199.00"));

        com.sqx.app.rental.user.domain.vo.UserCouponVo vo = UserCouponServiceImpl.toVo(coupon, template);

        assertEquals("CTM001", vo.getTemplateNo());
        assertEquals("新人立减券", vo.getName());
        assertEquals("满减券", vo.getCouponTypeLabel());
        assertTrue(vo.getConditionText().contains("199"));
    }

    @Test
    void consumeForOrderShouldSkipWhenCouponIdNull() {
        userCouponService.consumeForOrder(1L, null, "ORD001");
        verifyNoInteractions(userCouponMapper);
    }

    @Test
    void consumeForOrderShouldMarkCouponUsed() {
        UserCoupon coupon = buildUsableCoupon(1L, 10L);
        when(userCouponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.update(isNull(), any())).thenReturn(1);

        userCouponService.consumeForOrder(1L, 10L, "ORD001");

        verify(userCouponMapper).update(isNull(), any());
    }

    @Test
    void consumeForOrderShouldRejectUsedCoupon() {
        UserCoupon coupon = buildUsableCoupon(1L, 10L);
        coupon.setStatus(UserCouponStatus.USED.getCode());
        when(userCouponMapper.selectById(10L)).thenReturn(coupon);

        BizException ex = assertThrows(BizException.class,
                () -> userCouponService.consumeForOrder(1L, 10L, "ORD001"));
        assertTrue(ex.getMessage().contains("不可用"));
    }

    @Test
    void consumeForOrderShouldRejectExpiredCoupon() {
        UserCoupon coupon = buildUsableCoupon(1L, 10L);
        coupon.setExpireTime(LocalDateTime.now().minusDays(1));
        when(userCouponMapper.selectById(10L)).thenReturn(coupon);

        BizException ex = assertThrows(BizException.class,
                () -> userCouponService.consumeForOrder(1L, 10L, "ORD001"));
        assertTrue(ex.getMessage().contains("过期"));
    }

    @Test
    void consumeForOrderShouldRejectWhenConcurrentUpdateFails() {
        UserCoupon coupon = buildUsableCoupon(1L, 10L);
        when(userCouponMapper.selectById(10L)).thenReturn(coupon);
        when(userCouponMapper.update(isNull(), any())).thenReturn(0);

        BizException ex = assertThrows(BizException.class,
                () -> userCouponService.consumeForOrder(1L, 10L, "ORD001"));
        assertTrue(ex.getMessage().contains("核销失败"));
    }

    @Test
    void claimShouldInsertCouponWhenTemplateValid() {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setStatus(1);
        template.setValidEnd(LocalDateTime.now().plusDays(7));
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        when(userCouponMapper.update(isNull(), any())).thenReturn(0);
        when(userCouponMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(userCouponMapper.insert(any(UserCoupon.class))).thenReturn(1);

        userCouponService.claim(1L, 1L);

        verify(userCouponMapper).insert(any(UserCoupon.class));
    }

    @Test
    void claimShouldRejectWhenAlreadyClaimed() {
        CouponTemplate template = new CouponTemplate();
        template.setId(1L);
        template.setStatus(1);
        template.setValidEnd(LocalDateTime.now().plusDays(7));
        UserCoupon existing = buildUsableCoupon(1L, 10L);
        existing.setTemplateId(1L);
        when(couponTemplateMapper.selectById(1L)).thenReturn(template);
        when(userCouponMapper.update(isNull(), any())).thenReturn(0);
        when(userCouponMapper.selectList(any())).thenReturn(Collections.singletonList(existing));

        BizException ex = assertThrows(BizException.class, () -> userCouponService.claim(1L, 1L));
        assertTrue(ex.getMessage().contains("已领取"));
    }

    private static UserCoupon buildUsableCoupon(Long userId, Long couponId) {
        UserCoupon coupon = new UserCoupon();
        coupon.setId(couponId);
        coupon.setUserId(userId);
        coupon.setStatus(UserCouponStatus.UNUSED.getCode());
        coupon.setExpireTime(LocalDateTime.now().plusDays(7));
        return coupon;
    }
}
