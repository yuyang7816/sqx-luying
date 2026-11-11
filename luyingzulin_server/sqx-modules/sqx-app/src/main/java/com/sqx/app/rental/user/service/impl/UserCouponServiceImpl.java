package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.rental.user.domain.bo.UserCouponConsumeBo;
import com.sqx.app.rental.user.domain.bo.UserCouponPageBo;
import com.sqx.app.rental.user.domain.bo.UserCouponUsableBo;
import com.sqx.app.rental.user.domain.entity.CouponTemplate;
import com.sqx.app.rental.user.domain.entity.UserCoupon;
import com.sqx.app.rental.user.domain.vo.ClaimableCouponVo;
import com.sqx.app.rental.user.domain.vo.UserCouponVo;
import com.sqx.app.rental.user.enums.CouponType;
import com.sqx.app.rental.user.enums.UserCouponStatus;
import com.sqx.app.rental.user.mapper.CouponTemplateMapper;
import com.sqx.app.rental.user.mapper.UserCouponMapper;
import com.sqx.app.rental.user.service.UserCouponService;
import com.sqx.app.rental.user.support.CouponSupport;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserCouponServiceImpl implements UserCouponService {

    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private CouponTemplateMapper couponTemplateMapper;

    @Override
    public IPage<UserCouponVo> page(Long userId, UserCouponPageBo pageBo) {
        markExpiredCoupons(userId);
        Page<UserCoupon> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(StringUtils.isNotBlank(pageBo.getStatus()), UserCoupon::getStatus, pageBo.getStatus())
                .orderByDesc(UserCoupon::getCreateTime);
        IPage<UserCoupon> couponPage = userCouponMapper.selectPage(page, wrapper);
        return buildVoPage(couponPage, pageBo.getKeyword());
    }

    @Override
    public List<ClaimableCouponVo> listClaimable(Long userId) {
        markExpiredCoupons(userId);
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, 1)
                .and(w -> w.isNull(CouponTemplate::getValidStart).or().le(CouponTemplate::getValidStart, now))
                .and(w -> w.isNull(CouponTemplate::getValidEnd).or().ge(CouponTemplate::getValidEnd, now))
                .orderByDesc(CouponTemplate::getCreateTime));
        if (CollectionUtils.isEmpty(templates)) {
            return Collections.emptyList();
        }
        Set<Long> claimedTemplateIds = loadActiveTemplateIds(userId);
        return templates.stream()
                .map(template -> toClaimableVo(template, claimedTemplateIds.contains(template.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long templateId) {
        if (templateId == null) {
            throw new BizException("优惠券模板ID不能为空");
        }
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        validateClaimableTemplate(template);
        if (hasActiveCoupon(userId, templateId)) {
            throw new BizException("您已领取过该优惠券");
        }
        UserCoupon coupon = buildGrantCoupon(userId, template);
        userCouponMapper.insert(coupon);
    }

    @Override
    public List<UserCouponVo> listUsable(Long userId, UserCouponUsableBo usableBo) {
        markExpiredCoupons(userId);
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .gt(UserCoupon::getExpireTime, LocalDateTime.now())
                .orderByDesc(UserCoupon::getCreateTime));
        if (CollectionUtils.isEmpty(coupons)) {
            return Collections.emptyList();
        }
        Map<Long, CouponTemplate> templateMap = loadTemplateMap(coupons);
        BigDecimal rent = usableBo == null || usableBo.getRentAmount() == null ? BigDecimal.ZERO : usableBo.getRentAmount();
        BigDecimal delivery = usableBo == null || usableBo.getDeliveryAmount() == null ? BigDecimal.ZERO : usableBo.getDeliveryAmount();
        BigDecimal deposit = usableBo == null || usableBo.getDepositAmount() == null ? BigDecimal.ZERO : usableBo.getDepositAmount();

        List<UserCouponVo> result = new ArrayList<>();
        for (UserCoupon coupon : coupons) {
            CouponTemplate template = templateMap.get(coupon.getTemplateId());
            if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
                continue;
            }
            try {
                CouponSupport.calcDiscount(template, rent, delivery, deposit);
                result.add(toVo(coupon, template));
            } catch (BizException ignored) {
                // 不满足门槛的券不返回
            }
        }
        return result;
    }

    @Override
    public BigDecimal previewDiscount(Long userId, Long couponId, BigDecimal rentAmount,
                                      BigDecimal deliveryAmount, BigDecimal depositAmount) {
        if (couponId == null) {
            return BigDecimal.ZERO;
        }
        UserCoupon coupon = requireUsableCoupon(userId, couponId);
        CouponTemplate template = requireTemplate(coupon.getTemplateId());
        return CouponSupport.calcDiscount(template, rentAmount, deliveryAmount, depositAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consume(Long userId, UserCouponConsumeBo consumeBo) {
        consumeForOrder(userId, consumeBo.getCouponId(), consumeBo.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeForOrder(Long userId, Long couponId, String orderNo) {
        if (couponId == null) {
            return;
        }
        UserCoupon coupon = requireUsableCoupon(userId, couponId);
        int updated = userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getId, coupon.getId())
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .set(UserCoupon::getStatus, UserCouponStatus.USED.getCode())
                .set(UserCoupon::getOrderNo, orderNo)
                .set(UserCoupon::getUsedTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException("优惠券核销失败");
        }
    }

    @Override
    public void markExpiredCoupons(Long userId) {
        userCouponMapper.update(null, new LambdaUpdateWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .lt(UserCoupon::getExpireTime, LocalDateTime.now())
                .set(UserCoupon::getStatus, UserCouponStatus.EXPIRED.getCode()));
    }

    private UserCoupon requireUsableCoupon(Long userId, Long couponId) {
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !userId.equals(coupon.getUserId())) {
            throw new BizException("优惠券不存在");
        }
        if (!UserCouponStatus.UNUSED.getCode().equals(coupon.getStatus())) {
            throw new BizException("优惠券不可用");
        }
        if (coupon.getExpireTime() != null && coupon.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException("优惠券已过期");
        }
        return coupon;
    }

    private CouponTemplate requireTemplate(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BizException("优惠券模板不存在");
        }
        return template;
    }

    private IPage<UserCouponVo> buildVoPage(IPage<UserCoupon> couponPage, String keyword) {
        if (CollectionUtils.isEmpty(couponPage.getRecords())) {
            return PageUtil.copyPage(couponPage, Collections.emptyList());
        }
        Map<Long, CouponTemplate> templateMap = loadTemplateMap(couponPage.getRecords());
        List<UserCouponVo> records = couponPage.getRecords().stream()
                .map(c -> toVo(c, templateMap.get(c.getTemplateId())))
                .filter(vo -> StringUtils.isBlank(keyword)
                        || (vo.getName() != null && vo.getName().contains(keyword)))
                .collect(Collectors.toList());
        return PageUtil.copyPage(couponPage, records);
    }

    private Map<Long, CouponTemplate> loadTemplateMap(List<UserCoupon> coupons) {
        List<Long> templateIds = coupons.stream().map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templateIds)) {
            return Collections.emptyMap();
        }
        return couponTemplateMapper.selectBatchIds(templateIds).stream()
                .collect(Collectors.toMap(CouponTemplate::getId, Function.identity(), (a, b) -> a));
    }

    static UserCouponVo toVo(UserCoupon coupon, CouponTemplate template) {
        UserCouponVo vo = new UserCouponVo();
        vo.setId(coupon.getId());
        vo.setCouponNo(coupon.getCouponNo());
        vo.setStatus(coupon.getStatus());
        vo.setStatusLabel(resolveStatusLabel(coupon.getStatus()));
        vo.setExpireTime(coupon.getExpireTime());
        vo.setUsedTime(coupon.getUsedTime());
        vo.setOrderNo(coupon.getOrderNo());
        if (template != null) {
            vo.setTemplateNo(template.getTemplateNo());
            vo.setName(template.getName());
            vo.setCouponType(template.getCouponType());
            vo.setCouponTypeLabel(resolveTypeLabel(template.getCouponType()));
            vo.setAmount(template.getAmount());
            vo.setMinAmount(template.getMinAmount());
            vo.setConditionText(CouponSupport.buildConditionText(template));
        }
        return vo;
    }

    private static String resolveStatusLabel(String status) {
        try {
            return UserCouponStatus.fromCode(status).getLabel();
        } catch (Exception e) {
            return status;
        }
    }

    private static String resolveTypeLabel(String type) {
        try {
            return CouponType.fromCode(type).getLabel();
        } catch (Exception e) {
            return type;
        }
    }

    private void validateClaimableTemplate(CouponTemplate template) {
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new BizException("优惠券不可领取");
        }
        LocalDateTime now = LocalDateTime.now();
        if (template.getValidStart() != null && template.getValidStart().isAfter(now)) {
            throw new BizException("优惠券未到领取时间");
        }
        if (template.getValidEnd() != null && template.getValidEnd().isBefore(now)) {
            throw new BizException("优惠券已过期");
        }
    }

    private boolean hasActiveCoupon(Long userId, Long templateId) {
        return loadActiveTemplateIds(userId).contains(templateId);
    }

    private Set<Long> loadActiveTemplateIds(Long userId) {
        List<UserCoupon> coupons = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, UserCouponStatus.UNUSED.getCode())
                .gt(UserCoupon::getExpireTime, LocalDateTime.now()));
        if (CollectionUtils.isEmpty(coupons)) {
            return Collections.emptySet();
        }
        return coupons.stream()
                .map(UserCoupon::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private ClaimableCouponVo toClaimableVo(CouponTemplate template, boolean claimed) {
        ClaimableCouponVo vo = new ClaimableCouponVo();
        vo.setTemplateId(template.getId());
        vo.setName(template.getName());
        vo.setCouponType(template.getCouponType());
        vo.setCouponTypeLabel(resolveTypeLabel(template.getCouponType()));
        vo.setAmount(template.getAmount());
        vo.setMinAmount(template.getMinAmount());
        vo.setConditionText(CouponSupport.buildConditionText(template));
        vo.setValidStart(template.getValidStart());
        vo.setValidEnd(template.getValidEnd());
        vo.setClaimed(claimed);
        return vo;
    }

    public static UserCoupon buildGrantCoupon(Long userId, CouponTemplate template) {
        UserCoupon coupon = new UserCoupon();
        coupon.setCouponNo("CPN" + GenUtil.genOrderNo());
        coupon.setUserId(userId);
        coupon.setTemplateId(template.getId());
        coupon.setStatus(UserCouponStatus.UNUSED.getCode());
        coupon.setExpireTime(template.getValidEnd() != null ? template.getValidEnd()
                : LocalDateTime.now().plusDays(30));
        return coupon;
    }
}
