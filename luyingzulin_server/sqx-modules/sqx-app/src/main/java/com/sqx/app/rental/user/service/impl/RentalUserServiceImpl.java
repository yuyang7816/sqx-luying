package com.sqx.app.rental.user.service.impl;

import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.rental.user.domain.bo.UserProfileUpdateBo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.app.rental.user.service.RentalUserService;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 用户端资料服务实现
 */
@Service
public class RentalUserServiceImpl implements RentalUserService {

    @Resource
    private IUserService iUserService;

    @Override
    public AppUserInfoVo getInfo(Long userId) {
        UserEntity entity = requireUser(userId);
        return RentalUserSupport.toAppUserInfoVo(entity);
    }

    @Override
    public AppUserInfoVo updateProfile(Long userId, UserProfileUpdateBo updateBo) {
        UserEntity entity = requireUser(userId);
        if (StringUtils.isNotBlank(updateBo.getUserName())) {
            entity.setUserName(updateBo.getUserName().trim());
        }
        if (StringUtils.isNotBlank(updateBo.getAvatar())) {
            entity.setAvatar(updateBo.getAvatar().trim());
        }
        if (updateBo.getSex() != null) {
            entity.setSex(updateBo.getSex());
        }
        if (updateBo.getAge() != null) {
            entity.setAge(updateBo.getAge());
        }
        iUserService.updateById(entity);
        return RentalUserSupport.toAppUserInfoVo(requireUser(userId));
    }

    private UserEntity requireUser(Long userId) {
        UserEntity entity = iUserService.getById(userId);
        if (Objects.isNull(entity)) {
            throw new BizException(AppError.USER_NOT_FOUND);
        }
        return entity;
    }
}
