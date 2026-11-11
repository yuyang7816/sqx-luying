package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.bo.UpdateUserBO;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.referral.user.db.service.IReferralUserCommissionService;
import com.sqx.app.referral.user.domain.entity.ReferralUserCommission;
import com.sqx.app.rental.user.domain.bo.AdminUserPageBo;
import com.sqx.app.rental.user.domain.entity.UserAddress;
import com.sqx.app.rental.user.domain.entity.UserFavorite;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.app.rental.user.mapper.UserAddressMapper;
import com.sqx.app.rental.user.mapper.UserFavoriteMapper;
import com.sqx.app.rental.user.service.AdminRentalUserService;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端 C 端用户服务实现
 */
@Service
public class AdminRentalUserServiceImpl implements AdminRentalUserService {

    @Resource
    private IUserService iUserService;
    @Resource
    private UserAddressMapper userAddressMapper;
    @Resource
    private UserFavoriteMapper userFavoriteMapper;
    @Resource
    private IReferralUserCommissionService iReferralUserCommissionService;

    @Override
    public Page<AdminUserListVo> page(AdminUserPageBo pageBo) {
        Page<UserEntity> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(pageBo.getPhone()), UserEntity::getPhone, pageBo.getPhone())
                .like(StringUtils.isNotBlank(pageBo.getUsername()), UserEntity::getUserName, pageBo.getUsername())
                .eq(Objects.nonNull(pageBo.getStatus()), UserEntity::getStatus, pageBo.getStatus())
                .eq(Objects.nonNull(pageBo.getUserType()), UserEntity::getUserType, pageBo.getUserType())
                .eq(Objects.nonNull(pageBo.getIsVip()), UserEntity::getUserType,
                        Integer.valueOf(1).equals(pageBo.getIsVip()) ? 2 : 1)
                .eq(StringUtils.isNotBlank(pageBo.getPlatform()), UserEntity::getPlatform, pageBo.getPlatform())
                .eq(StringUtils.isNotBlank(pageBo.getInvitationCode()), UserEntity::getInvitationCode, pageBo.getInvitationCode())
                .ge(Objects.nonNull(pageBo.getCreateTimeStart()), UserEntity::getCreateTime, pageBo.getCreateTimeStart())
                .le(Objects.nonNull(pageBo.getCreateTimeEnd()), UserEntity::getCreateTime, pageBo.getCreateTimeEnd())
                .orderByDesc(UserEntity::getCreateTime);
        Page<UserEntity> entityPage = iUserService.page(page, queryWrapper);

        List<UserEntity> records = entityPage.getRecords();
        Map<String, UserEntity> inviterByCode = loadInviterMap(records);
        Map<String, List<UserEntity>> inviteesByCode = loadInviteesMap(records);
        Map<Long, ReferralUserCommission> commissionByUserId = loadCommissionMap(records);

        Page<AdminUserListVo> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(records.stream()
                .map(entity -> toAdminUserListVoWithReferral(entity, inviterByCode, inviteesByCode, commissionByUserId))
                .collect(Collectors.toList()));
        return voPage;
    }

    private Map<String, UserEntity> loadInviterMap(List<UserEntity> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<String> superiorCodes = records.stream()
                .map(UserEntity::getSuperiorInvitationCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(superiorCodes)) {
            return Collections.emptyMap();
        }
        return iUserService.list(new LambdaQueryWrapper<UserEntity>()
                        .in(UserEntity::getInvitationCode, superiorCodes))
                .stream()
                .collect(Collectors.toMap(UserEntity::getInvitationCode, user -> user, (left, right) -> left));
    }

    private Map<String, List<UserEntity>> loadInviteesMap(List<UserEntity> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<String> invitationCodes = records.stream()
                .map(UserEntity::getInvitationCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(invitationCodes)) {
            return Collections.emptyMap();
        }
        return iUserService.list(new LambdaQueryWrapper<UserEntity>()
                        .in(UserEntity::getSuperiorInvitationCode, invitationCodes)
                        .orderByDesc(UserEntity::getCreateTime))
                .stream()
                .collect(Collectors.groupingBy(UserEntity::getSuperiorInvitationCode));
    }

    private Map<Long, ReferralUserCommission> loadCommissionMap(List<UserEntity> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        List<Long> userIds = records.stream()
                .map(UserEntity::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return iReferralUserCommissionService.list(new LambdaQueryWrapper<ReferralUserCommission>()
                        .in(ReferralUserCommission::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(ReferralUserCommission::getUserId, item -> item, (left, right) -> left));
    }

    private AdminUserListVo toAdminUserListVoWithReferral(UserEntity entity,
                                                          Map<String, UserEntity> inviterByCode,
                                                          Map<String, List<UserEntity>> inviteesByCode,
                                                          Map<Long, ReferralUserCommission> commissionByUserId) {
        AdminUserListVo vo = RentalUserSupport.toAdminUserListVo(entity);
        if (StringUtils.isNotBlank(entity.getSuperiorInvitationCode())) {
            UserEntity inviter = inviterByCode.get(entity.getSuperiorInvitationCode());
            if (inviter != null) {
                vo.setInviterUserId(inviter.getUserId());
                vo.setInviterUserName(inviter.getUserName());
                vo.setInviterPhone(inviter.getPhone());
            }
        }
        List<UserEntity> invitees = inviteesByCode.getOrDefault(entity.getInvitationCode(), Collections.emptyList());
        vo.setInviteCount((long) invitees.size());
        vo.setInviteeSummary(buildInviteeSummary(invitees));
        ReferralUserCommission commission = commissionByUserId.get(entity.getUserId());
        if (commission != null) {
            vo.setCommissionId(commission.getId());
            vo.setCommission(commission.getCommission());
        } else {
            vo.setCommission(BigDecimal.ZERO);
        }
        return vo;
    }

    private String buildInviteeSummary(List<UserEntity> invitees) {
        if (CollectionUtils.isEmpty(invitees)) {
            return "";
        }
        return invitees.stream()
                .limit(3)
                .map(this::resolveInviteeLabel)
                .collect(Collectors.joining("、"));
    }

    private String resolveInviteeLabel(UserEntity invitee) {
        if (StringUtils.isNotBlank(invitee.getUserName())) {
            return invitee.getUserName();
        }
        return RentalUserSupport.maskPhone(invitee.getPhone());
    }

    @Override
    public AppUserInfoVo info(Long userId) {
        UserEntity entity = iUserService.getById(userId);
        if (Objects.isNull(entity)) {
            throw new BizException(AppError.USER_NOT_FOUND);
        }
        AppUserInfoVo vo = RentalUserSupport.toAdminAppUserInfoVo(entity);
        vo.setAddressCount(userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)).intValue());
        vo.setFavoriteCount(userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)).intValue());
        return vo;
    }

    @Override
    public void update(UpdateUserBO updateUserBO) {
        UserEntity existing = iUserService.getById(updateUserBO.getUserId());
        if (Objects.isNull(existing)) {
            throw new BizException(AppError.USER_NOT_FOUND);
        }
        if (StringUtils.isNotBlank(updateUserBO.getPhone())) {
            String phone = updateUserBO.getPhone().trim();
            if (!Objects.equals(phone, StringUtils.defaultString(existing.getPhone()))) {
                throw new BizException("登录手机号不可修改");
            }
        }
        updateUserBO.setPhone(null);
        if (Objects.nonNull(updateUserBO.getIsVip())) {
            updateUserBO.setUserType(Integer.valueOf(1).equals(updateUserBO.getIsVip()) ? 2 : 1);
        }
        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(updateUserBO, entity);
        iUserService.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long userId) {
        iUserService.removeById(userId);
    }
}
