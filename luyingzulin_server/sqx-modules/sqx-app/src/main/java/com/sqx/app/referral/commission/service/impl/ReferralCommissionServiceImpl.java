package com.sqx.app.referral.commission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.referral.commission.db.service.IReferralCommissionService;
import com.sqx.app.referral.commission.domain.bo.ReferralCommissionInfoBo;
import com.sqx.app.referral.commission.domain.bo.ReferralCommissionPageBo;
import com.sqx.app.referral.commission.domain.bo.ReferralCommissionSaveBo;
import com.sqx.app.referral.commission.domain.bo.ReferralCommissionUpdateBo;
import com.sqx.app.referral.commission.domain.entity.ReferralCommission;
import com.sqx.app.referral.commission.domain.vo.ReferralCommissionVo;
import com.sqx.app.referral.commission.service.ReferralCommissionService;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReferralCommissionServiceImpl implements ReferralCommissionService {

    @Resource
    private IReferralCommissionService iReferralCommissionService;
    @Resource
    private IUserService iUserService;
    @Resource
    private RentOrderMapper rentOrderMapper;

    @Override
    public void saveInfo(ReferralCommissionSaveBo referralCommissionSaveBo) {
        ReferralCommission referralCommission = new ReferralCommission();
        BeanUtils.copyProperties(referralCommissionSaveBo, referralCommission);
        iReferralCommissionService.save(referralCommission);
    }

    @Override
    public IPage<ReferralCommissionVo> pageInfo(ReferralCommissionPageBo referralCommissionPageBo) {
        Page<ReferralCommission> initPage = PageUtil.initPage(referralCommissionPageBo.getPage(), referralCommissionPageBo.getLimit());

        QueryWrapper<ReferralCommission> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(Objects.nonNull(referralCommissionPageBo.getUserId()), ReferralCommission::getUserId, referralCommissionPageBo.getUserId())
                .orderByDesc(ReferralCommission::getCreateTime);
        if (StringUtils.isNotBlank(referralCommissionPageBo.getPhone())) {
            List<UserEntity> users = iUserService.list(new LambdaQueryWrapper<UserEntity>()
                    .like(UserEntity::getPhone, referralCommissionPageBo.getPhone()));
            if (CollectionUtils.isEmpty(users)) {
                return PageUtil.copyPage(initPage, Collections.emptyList());
            }
            List<Long> userIds = users.stream().map(UserEntity::getUserId).collect(Collectors.toList());
            queryWrapper.lambda().in(ReferralCommission::getUserId, userIds);
        }

        IPage<ReferralCommission> iPage = iReferralCommissionService.page(initPage, queryWrapper);
        List<ReferralCommissionVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            List<Long> userIds = iPage.getRecords().stream().map(ReferralCommission::getUserId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            List<Long> orderIds = iPage.getRecords().stream().map(ReferralCommission::getOrderId)
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            Map<Long, UserEntity> userMap = CollectionUtils.isEmpty(userIds) ? Collections.emptyMap()
                    : iUserService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
            Map<Long, RentOrder> orderMap = CollectionUtils.isEmpty(orderIds) ? Collections.emptyMap()
                    : rentOrderMapper.selectBatchIds(orderIds).stream()
                    .collect(Collectors.toMap(RentOrder::getId, o -> o, (a, b) -> a));
            for (ReferralCommission record : iPage.getRecords()) {
                ReferralCommissionVo vo = new ReferralCommissionVo();
                BeanUtils.copyProperties(record, vo);
                UserEntity user = userMap.get(record.getUserId());
                if (user != null) {
                    vo.setUserPhone(RentalUserSupport.maskPhone(user.getPhone()));
                    vo.setUserName(user.getUserName());
                }
                RentOrder order = orderMap.get(record.getOrderId());
                if (order != null) {
                    vo.setOrderNo(order.getOrderNo());
                }
                vo.setReferralLevel(resolveReferralLevel(record.getTitle()));
                voList.add(vo);
            }
        }
        return PageUtil.copyPage(iPage, voList);
    }

    @Override
    public ReferralCommissionVo info(ReferralCommissionInfoBo referralCommissionInfoBo) {
        ReferralCommission referralCommission = iReferralCommissionService.getById(referralCommissionInfoBo.getId());
        if (Objects.isNull(referralCommission)) {
            throw new BizException("信息不存在");
        }
        ReferralCommissionVo vo = new ReferralCommissionVo();
        BeanUtils.copyProperties(referralCommission, vo);
        vo.setReferralLevel(resolveReferralLevel(referralCommission.getTitle()));
        if (referralCommission.getUserId() != null) {
            UserEntity user = iUserService.getById(referralCommission.getUserId());
            if (user != null) {
                vo.setUserPhone(user.getPhone());
                vo.setUserName(user.getUserName());
            }
        }
        if (referralCommission.getOrderId() != null) {
            RentOrder order = rentOrderMapper.selectById(referralCommission.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
            }
        }
        return vo;
    }

    @Override
    public void updateInfo(ReferralCommissionUpdateBo referralCommissionUpdateBo) {
        ReferralCommission referralCommission = new ReferralCommission();
        BeanUtils.copyProperties(referralCommissionUpdateBo, referralCommission);
        iReferralCommissionService.updateById(referralCommission);
    }

    @Override
    public void delete(ReferralCommissionInfoBo referralCommissionInfoBo) {
        iReferralCommissionService.removeById(referralCommissionInfoBo.getId());
    }

    private static String resolveReferralLevel(String title) {
        if (StringUtils.isBlank(title)) {
            return "-";
        }
        if (title.contains("二级")) {
            return "二级";
        }
        if (title.contains("一级")) {
            return "一级";
        }
        return "一级";
    }
}
