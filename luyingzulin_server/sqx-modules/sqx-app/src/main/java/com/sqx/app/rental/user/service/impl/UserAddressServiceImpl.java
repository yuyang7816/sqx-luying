package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.rental.user.domain.bo.UserAddressSaveBo;
import com.sqx.app.rental.user.domain.entity.UserAddress;
import com.sqx.app.rental.user.domain.vo.UserAddressVo;
import com.sqx.app.rental.user.mapper.UserAddressMapper;
import com.sqx.app.rental.user.service.UserAddressService;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户配送地址服务实现
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Resource
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddressVo> list(Long userId) {
        List<UserAddress> addresses = userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getUpdateTime));
        if (CollectionUtils.isEmpty(addresses)) {
            return Collections.emptyList();
        }
        return addresses.stream().map(RentalUserSupport::toAddressVo).collect(Collectors.toList());
    }

    @Override
    public UserAddressVo detail(Long userId, Long addressId) {
        return RentalUserSupport.toAddressVo(requireOwnedAddress(userId, addressId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAddressVo save(Long userId, UserAddressSaveBo saveBo) {
        boolean setDefault = Boolean.TRUE.equals(saveBo.getIsDefault());
        UserAddress address;
        if (saveBo.getId() == null) {
            address = new UserAddress();
            address.setUserId(userId);
            applyAddressFields(address, saveBo);
            long existingCount = countByUser(userId);
            if (setDefault || existingCount == 0) {
                if (existingCount > 0) {
                    clearDefault(userId);
                }
                address.setIsDefault(1);
            } else {
                address.setIsDefault(0);
            }
            userAddressMapper.insert(address);
        } else {
            address = requireOwnedAddress(userId, saveBo.getId());
            applyAddressFields(address, saveBo);
            if (setDefault) {
                address.setIsDefault(1);
                clearDefaultExcept(userId, address.getId());
            }
            userAddressMapper.updateById(address);
        }
        return RentalUserSupport.toAddressVo(requireOwnedAddress(userId, address.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long addressId) {
        UserAddress address = requireOwnedAddress(userId, addressId);
        long total = countByUser(userId);
        if (total <= 1) {
            throw new BizException("至少保留一个收货地址");
        }
        userAddressMapper.deleteById(address.getId());
        if (Integer.valueOf(1).equals(address.getIsDefault())) {
            UserAddress next = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                    .eq(UserAddress::getUserId, userId)
                    .orderByDesc(UserAddress::getUpdateTime)
                    .last("LIMIT 1"));
            if (next != null) {
                next.setIsDefault(1);
                userAddressMapper.updateById(next);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long addressId) {
        requireOwnedAddress(userId, addressId);
        clearDefault(userId);
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 1));
    }

    private void applyAddressFields(UserAddress address, UserAddressSaveBo saveBo) {
        address.setContactName(saveBo.getContactName().trim());
        address.setContactPhone(saveBo.getContactPhone().trim());
        address.setProvince(trimToNull(saveBo.getProvince()));
        address.setCity(trimToNull(saveBo.getCity()));
        address.setDistrict(trimToNull(saveBo.getDistrict()));
        address.setDetailAddress(saveBo.getDetailAddress().trim());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private long countByUser(Long userId) {
        return userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId));
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, 0));
    }

    private void clearDefaultExcept(Long userId, Long keepId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .ne(UserAddress::getId, keepId)
                .set(UserAddress::getIsDefault, 0));
    }

    private UserAddress requireOwnedAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectOne(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getId, addressId)
                .eq(UserAddress::getUserId, userId)
                .last("LIMIT 1"));
        if (Objects.isNull(address)) {
            throw new BizException("地址不存在");
        }
        return address;
    }
}
