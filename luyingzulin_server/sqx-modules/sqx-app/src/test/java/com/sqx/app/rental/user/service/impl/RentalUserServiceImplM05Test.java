package com.sqx.app.rental.user.service.impl;

import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.rental.user.domain.bo.UserProfileUpdateBo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * M05：用户资料更新字段校验单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentalUserServiceImplM05Test {

    @Mock
    private IUserService iUserService;

    @InjectMocks
    private RentalUserServiceImpl rentalUserService;

    @Test
    void updateProfileShouldTrimUserNameAndReturnMaskedPhone() {
        UserEntity entity = buildUser(1L, "camp001", "13812348888");
        when(iUserService.getById(1L)).thenReturn(entity);

        UserProfileUpdateBo updateBo = new UserProfileUpdateBo();
        updateBo.setUserName("  露营达人  ");

        AppUserInfoVo vo = rentalUserService.updateProfile(1L, updateBo);

        verify(iUserService).updateById(argThat(user -> "露营达人".equals(user.getUserName())));
        assertEquals("138****8888", vo.getPhoneMasked());
    }

    @Test
    void updateProfileShouldSkipBlankOptionalFields() {
        UserEntity entity = buildUser(1L, "camp001", "13812348888");
        entity.setAvatar("/old.png");
        entity.setSex(1);
        entity.setAge(25);
        when(iUserService.getById(1L)).thenReturn(entity);

        UserProfileUpdateBo updateBo = new UserProfileUpdateBo();
        updateBo.setUserName("  ");
        updateBo.setAvatar("");

        rentalUserService.updateProfile(1L, updateBo);

        verify(iUserService).updateById(argThat(user ->
                "camp001".equals(user.getUserName())
                        && "/old.png".equals(user.getAvatar())
                        && Integer.valueOf(1).equals(user.getSex())
                        && Integer.valueOf(25).equals(user.getAge())));
    }

    @Test
    void updateProfileShouldApplySexAndAge() {
        UserEntity entity = buildUser(2L, "camp002", "13912341234");
        when(iUserService.getById(2L)).thenReturn(entity);

        UserProfileUpdateBo updateBo = new UserProfileUpdateBo();
        updateBo.setSex(2);
        updateBo.setAge(30);
        updateBo.setAvatar(" /avatar/new.png ");

        AppUserInfoVo vo = rentalUserService.updateProfile(2L, updateBo);

        verify(iUserService).updateById(argThat(user ->
                Integer.valueOf(2).equals(user.getSex())
                        && Integer.valueOf(30).equals(user.getAge())
                        && "/avatar/new.png".equals(user.getAvatar())));
        assertEquals(2, vo.getSex());
        assertEquals(30, vo.getAge());
    }

    @Test
    void updateProfileShouldRejectMissingUser() {
        when(iUserService.getById(99L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class,
                () -> rentalUserService.updateProfile(99L, new UserProfileUpdateBo()));
        assertEquals(AppError.USER_NOT_FOUND, ex.getErrorCode());
    }

    private UserEntity buildUser(Long userId, String userName, String phone) {
        UserEntity entity = new UserEntity();
        entity.setUserId(userId);
        entity.setUserName(userName);
        entity.setPhone(phone);
        return entity;
    }
}
