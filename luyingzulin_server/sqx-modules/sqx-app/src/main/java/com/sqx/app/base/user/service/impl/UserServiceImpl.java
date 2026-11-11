package com.sqx.app.base.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.bo.DeleteUserBO;
import com.sqx.app.base.user.domain.bo.ForgetPasswordBO;
import com.sqx.app.base.user.domain.bo.ListUserBO;
import com.sqx.app.base.user.domain.bo.UpdateUserBO;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.domain.vo.UserVo;
import com.sqx.app.base.user.service.UserService;
import com.sqx.app.base.verifyCode.service.VerifyCodeService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author chengming
 * @since 2025/4/24
 */
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private IUserService iUserService;
    @Resource
    private VerifyCodeService verifyCodeService;

    @Override
    public Result<UserVo> info(Long userId) {
        UserEntity userEntity = iUserService.getById(userId);
        if (Objects.isNull(userEntity)) {
            throw new BizException(AppError.USER_NOT_FOUND);
        }

        UserVo vo = new UserVo();
        BeanUtils.copyProperties(userEntity, vo);
        return Result.success(vo);
    }

    @Override
    public Result<Void> forgetPassword(ForgetPasswordBO forgetPasswordBO) {
        UserEntity userEntity = iUserService.getById(forgetPasswordBO.getUserId());
        if (Objects.isNull(userEntity)) {
            throw new BizException(AppError.USER_NOT_FOUND);
        }

        if (!verifyCodeService.verify(userEntity.getPhone(), forgetPasswordBO.getCode())) {
            throw new BizException(AppError.VERIFY_CODE_ERROR);
        }

        iUserService.update(new UserEntity(), new LambdaUpdateWrapper<UserEntity>()
                .eq(UserEntity::getUserId, forgetPasswordBO.getUserId())
                .set(UserEntity::getPassword, DigestUtils.md5Hex(forgetPasswordBO.getPassword()))
        );

        return Result.success();
    }

    @Override
    public Page<UserEntity> list(ListUserBO listUserBO) {
        Page<UserEntity> initPage = PageUtil.initPage(listUserBO.getPage(), listUserBO.getLimit());

        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(StringUtils.isNotBlank(listUserBO.getPhone()), UserEntity::getPhone, listUserBO.getPhone())
                .eq(StringUtils.isNotBlank(listUserBO.getUsername()), UserEntity::getUserName, listUserBO.getUsername())
                .eq(Objects.nonNull(listUserBO.getStatus()), UserEntity::getStatus, listUserBO.getStatus())
                .eq(Objects.nonNull(listUserBO.getUserType()), UserEntity::getUserType, listUserBO.getUserType())
                .orderByDesc(UserEntity::getCreateTime);

        return iUserService.page(initPage, queryWrapper);
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
        UserEntity userEntity = new UserEntity();
        BeanUtils.copyProperties(updateUserBO, userEntity);
        iUserService.updateById(userEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(DeleteUserBO deleteUserBO) {
        iUserService.removeById(deleteUserBO.getUserId());

        // 其他需要删除的信息
    }
}
