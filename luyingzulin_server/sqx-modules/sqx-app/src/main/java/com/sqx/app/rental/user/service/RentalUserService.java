package com.sqx.app.rental.user.service;

import com.sqx.app.rental.user.domain.bo.UserProfileUpdateBo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;

/**
 * 用户端资料服务
 */
public interface RentalUserService {

    AppUserInfoVo getInfo(Long userId);

    AppUserInfoVo updateProfile(Long userId, UserProfileUpdateBo updateBo);
}
