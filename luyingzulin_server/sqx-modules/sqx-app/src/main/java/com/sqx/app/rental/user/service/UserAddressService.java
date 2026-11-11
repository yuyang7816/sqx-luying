package com.sqx.app.rental.user.service;

import com.sqx.app.rental.user.domain.bo.UserAddressSaveBo;
import com.sqx.app.rental.user.domain.vo.UserAddressVo;

import java.util.List;

/**
 * 用户配送地址服务
 */
public interface UserAddressService {

    List<UserAddressVo> list(Long userId);

    UserAddressVo detail(Long userId, Long addressId);

    UserAddressVo save(Long userId, UserAddressSaveBo saveBo);

    void delete(Long userId, Long addressId);

    void setDefault(Long userId, Long addressId);
}
