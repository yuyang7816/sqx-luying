package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.rental.user.domain.bo.CouponGrantBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplatePageBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplateSaveBo;
import com.sqx.app.rental.user.domain.entity.CouponTemplate;
import com.sqx.app.rental.user.domain.entity.UserCoupon;
import com.sqx.app.rental.user.domain.vo.CouponTemplateVo;
import com.sqx.app.rental.user.enums.CouponType;
import com.sqx.app.rental.user.mapper.CouponTemplateMapper;
import com.sqx.app.rental.user.mapper.UserCouponMapper;
import com.sqx.app.rental.user.service.CouponTemplateService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponTemplateServiceImpl implements CouponTemplateService {

    @Resource
    private CouponTemplateMapper couponTemplateMapper;
    @Resource
    private UserCouponMapper userCouponMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    public IPage<CouponTemplateVo> page(CouponTemplatePageBo pageBo) {
        Page<CouponTemplate> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(CouponTemplate::getId, id);
            } else {
                wrapper.like(CouponTemplate::getName, pageBo.getKeyword());
            }
        }
        wrapper.eq(StringUtils.isNotBlank(pageBo.getCouponType()), CouponTemplate::getCouponType, pageBo.getCouponType())
                .eq(pageBo.getStatus() != null, CouponTemplate::getStatus, pageBo.getStatus())
                .orderByDesc(CouponTemplate::getCreateTime);
        IPage<CouponTemplate> templatePage = couponTemplateMapper.selectPage(page, wrapper);
        List<CouponTemplateVo> records = templatePage.getRecords().stream()
                .map(CouponTemplateServiceImpl::toVo).collect(Collectors.toList());
        return PageUtil.copyPage(templatePage, records);
    }

    @Override
    public CouponTemplateVo info(Long id) {
        CouponTemplate template = couponTemplateMapper.selectById(id);
        if (template == null) {
            throw new BizException("优惠券模板不存在");
        }
        return toVo(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CouponTemplateSaveBo saveBo) {
        CouponType.fromCode(saveBo.getCouponType());
        if (StringUtils.isBlank(saveBo.getTemplateNo())) {
            saveBo.setTemplateNo("CP" + GenUtil.genOrderNo());
        }
        CouponTemplate existing = couponTemplateMapper.selectOne(new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getTemplateNo, saveBo.getTemplateNo())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException("模板编号已存在");
        }
        CouponTemplate template = new CouponTemplate();
        BeanUtils.copyProperties(saveBo, template);
        if (template.getStatus() == null) {
            template.setStatus(1);
        }
        couponTemplateMapper.insert(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CouponTemplateSaveBo saveBo) {
        if (saveBo.getId() == null) {
            throw new BizException("模板ID不能为空");
        }
        CouponTemplate template = couponTemplateMapper.selectById(saveBo.getId());
        if (template == null) {
            throw new BizException("优惠券模板不存在");
        }
        CouponType.fromCode(saveBo.getCouponType());
        BeanUtils.copyProperties(saveBo, template, "templateNo");
        couponTemplateMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        couponTemplateMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grant(CouponGrantBo grantBo) {
        CouponTemplate template = couponTemplateMapper.selectById(grantBo.getTemplateId());
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new BizException("优惠券模板不可用");
        }
        List<Long> userIds = resolveGrantUserIds(grantBo);
        if (CollectionUtils.isEmpty(userIds)) {
            throw new BizException("请指定发放用户");
        }
        int count = 0;
        for (Long userId : userIds) {
            UserCoupon coupon = UserCouponServiceImpl.buildGrantCoupon(userId, template);
            userCouponMapper.insert(coupon);
            count++;
        }
        return count;
    }

    private List<Long> resolveGrantUserIds(CouponGrantBo grantBo) {
        List<Long> userIds = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(grantBo.getUserIds())) {
            userIds.addAll(grantBo.getUserIds());
        }
        if (CollectionUtils.isNotEmpty(grantBo.getPhones())) {
            for (String phone : grantBo.getPhones()) {
                UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getPhone, phone).last("LIMIT 1"));
                if (user != null) {
                    userIds.add(user.getUserId());
                }
            }
        }
        return userIds.stream().distinct().collect(Collectors.toList());
    }

    static CouponTemplateVo toVo(CouponTemplate template) {
        CouponTemplateVo vo = new CouponTemplateVo();
        BeanUtils.copyProperties(template, vo);
        try {
            vo.setCouponTypeLabel(CouponType.fromCode(template.getCouponType()).getLabel());
        } catch (Exception e) {
            vo.setCouponTypeLabel(template.getCouponType());
        }
        vo.setStatusLabel(Integer.valueOf(1).equals(template.getStatus()) ? "启用" : "停用");
        return vo;
    }
}
