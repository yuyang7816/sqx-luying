package com.sqx.app.merchant.staff.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.staff.domain.bo.MerchantStaffSaveBo;
import com.sqx.app.merchant.staff.domain.entity.MerchantStaff;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffProfileVo;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffVo;
import com.sqx.app.merchant.staff.mapper.MerchantStaffMapper;
import com.sqx.app.merchant.staff.service.MerchantStaffService;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantStaffServiceImpl implements MerchantStaffService {

    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private MerchantStaffMapper merchantStaffMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    public List<MerchantStaffVo> list(Long userId) {
        assertOwner(userId);
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        List<MerchantStaff> staffList = merchantStaffMapper.selectList(new LambdaQueryWrapper<MerchantStaff>()
                .eq(MerchantStaff::getMerchantId, ctx.getMerchantId())
                .orderByAsc(MerchantStaff::getId));
        return staffList.stream().map(staff -> {
            MerchantStaffVo vo = new MerchantStaffVo();
            BeanUtils.copyProperties(staff, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public MerchantStaffProfileVo profile(Long userId) {
        if (isMerchantOwner(userId)) {
            return inactiveProfile();
        }
        MerchantStaff staff = resolveActiveStaff(userId, true);
        if (staff == null) {
            return inactiveProfile();
        }
        return toProfileVo(staff);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long userId, MerchantStaffSaveBo saveBo) {
        assertOwner(userId);
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        Long storeId = resolveStoreId(ctx.getMerchantId(), saveBo.getStoreId());

        if (saveBo.getId() != null) {
            MerchantStaff existing = merchantStaffMapper.selectById(saveBo.getId());
            if (existing == null || !ctx.getMerchantId().equals(existing.getMerchantId())) {
                throw new BizException("员工不存在或无权编辑");
            }
            existing.setStoreId(storeId);
            existing.setName(saveBo.getName());
            existing.setRole(saveBo.getRole());
            existing.setPhone(saveBo.getPhone());
            existing.setStatus(saveBo.getStatus());
            bindUserIdIfPossible(existing);
            merchantStaffMapper.updateById(existing);
            return;
        }

        MerchantStaff staff = new MerchantStaff();
        staff.setMerchantId(ctx.getMerchantId());
        staff.setStoreId(storeId);
        staff.setName(saveBo.getName());
        staff.setRole(saveBo.getRole());
        staff.setPhone(saveBo.getPhone());
        staff.setStatus(saveBo.getStatus());
        bindUserIdIfPossible(staff);
        merchantStaffMapper.insert(staff);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long staffId) {
        assertOwner(userId);
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        MerchantStaff staff = merchantStaffMapper.selectById(staffId);
        if (staff == null || !ctx.getMerchantId().equals(staff.getMerchantId())) {
            throw new BizException("员工不存在或无权删除");
        }
        merchantStaffMapper.deleteById(staffId);
    }

    private void assertOwner(Long userId) {
        merchantContextSupport.requireMerchant(userId);
    }

    private boolean isMerchantOwner(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        return merchant != null;
    }

    private MerchantStaff resolveActiveStaff(Long userId, boolean bindByPhone) {
        MerchantStaff staff = merchantStaffMapper.selectOne(new LambdaQueryWrapper<MerchantStaff>()
                .eq(MerchantStaff::getUserId, userId)
                .eq(MerchantStaff::getStatus, 1)
                .last("LIMIT 1"));
        if (staff != null) {
            return staff;
        }
        if (!bindByPhone) {
            return null;
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null || StringUtils.isBlank(user.getPhone())) {
            return null;
        }
        staff = merchantStaffMapper.selectOne(new LambdaQueryWrapper<MerchantStaff>()
                .eq(MerchantStaff::getPhone, user.getPhone().trim())
                .eq(MerchantStaff::getStatus, 1)
                .orderByDesc(MerchantStaff::getId)
                .last("LIMIT 1"));
        if (staff == null) {
            return null;
        }
        if (staff.getUserId() == null || !userId.equals(staff.getUserId())) {
            staff.setUserId(userId);
            merchantStaffMapper.updateById(staff);
        }
        return staff;
    }

    private void bindUserIdIfPossible(MerchantStaff staff) {
        if (staff == null || StringUtils.isBlank(staff.getPhone())) {
            return;
        }
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getPhone, staff.getPhone().trim())
                .orderByDesc(UserEntity::getUserId)
                .last("LIMIT 1"));
        if (user != null) {
            staff.setUserId(user.getUserId());
        }
    }

    private MerchantStaffProfileVo inactiveProfile() {
        MerchantStaffProfileVo vo = new MerchantStaffProfileVo();
        vo.setActive(false);
        return vo;
    }

    private MerchantStaffProfileVo toProfileVo(MerchantStaff staff) {
        Merchant merchant = merchantMapper.selectById(staff.getMerchantId());
        MerchantStore store = staff.getStoreId() == null ? null : merchantStoreMapper.selectById(staff.getStoreId());
        MerchantStaffProfileVo vo = new MerchantStaffProfileVo();
        vo.setActive(true);
        vo.setStaffId(staff.getId());
        vo.setName(staff.getName());
        vo.setRole(staff.getRole());
        vo.setPhone(staff.getPhone());
        vo.setMerchantId(staff.getMerchantId());
        vo.setStoreId(staff.getStoreId());
        vo.setMerchantName(merchant == null ? null : merchant.getName());
        vo.setStoreName(store == null ? null : store.getName());
        return vo;
    }

    private Long resolveStoreId(Long merchantId, Long storeId) {
        if (storeId != null) {
            MerchantStore store = merchantStoreMapper.selectById(storeId);
            if (store == null || !merchantId.equals(store.getMerchantId())) {
                throw new BizException("门店不存在");
            }
            return storeId;
        }
        MerchantStore defaultStore = merchantStoreMapper.selectOne(new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getMerchantId, merchantId)
                .orderByAsc(MerchantStore::getId)
                .last("LIMIT 1"));
        if (defaultStore == null) {
            throw new BizException("请先配置门店信息");
        }
        return defaultStore.getId();
    }
}
