package com.sqx.app.merchant.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.staff.domain.entity.MerchantStaff;
import com.sqx.app.merchant.staff.domain.vo.MerchantStaffVo;
import com.sqx.app.merchant.staff.mapper.MerchantStaffMapper;
import com.sqx.app.merchant.store.domain.bo.MerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.domain.vo.MerchantStoreDetailVo;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.store.service.MerchantStoreService;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.common.core.exception.BizException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MerchantStoreServiceImpl implements MerchantStoreService {

    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;
    @Resource
    private MerchantStaffMapper merchantStaffMapper;

    @Override
    public MerchantStoreDetailVo detail(Long userId) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        MerchantStore store = merchantStoreMapper.selectOne(new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getMerchantId, ctx.getMerchantId())
                .orderByAsc(MerchantStore::getId)
                .last("LIMIT 1"));
        if (store == null) {
            throw new BizException("门店信息不存在，请联系平台");
        }
        return toDetailVo(store);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long userId, MerchantStoreSaveBo saveBo) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        if (saveBo.getId() != null) {
            MerchantStore existing = merchantStoreMapper.selectById(saveBo.getId());
            if (existing == null || !ctx.getMerchantId().equals(existing.getMerchantId())) {
                throw new BizException("门店不存在或无权编辑");
            }
            copyFields(saveBo, existing);
            merchantStoreMapper.updateById(existing);
            return;
        }

        MerchantStore store = new MerchantStore();
        store.setStoreNo("STORE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        store.setMerchantId(ctx.getMerchantId());
        copyFields(saveBo, store);
        if (store.getStatus() == null) {
            store.setStatus(1);
        }
        if (store.getDeliveryEnabled() == null) {
            store.setDeliveryEnabled(1);
        }
        if (store.getSelfPickupEnabled() == null) {
            store.setSelfPickupEnabled(1);
        }
        merchantStoreMapper.insert(store);
    }

    private MerchantStoreDetailVo toDetailVo(MerchantStore store) {
        MerchantStoreDetailVo vo = new MerchantStoreDetailVo();
        BeanUtils.copyProperties(store, vo);
        List<MerchantStaff> staffList = merchantStaffMapper.selectList(new LambdaQueryWrapper<MerchantStaff>()
                .eq(MerchantStaff::getMerchantId, store.getMerchantId())
                .eq(MerchantStaff::getStoreId, store.getId())
                .orderByAsc(MerchantStaff::getId));
        vo.setStaffList(staffList.stream().map(staff -> {
            MerchantStaffVo staffVo = new MerchantStaffVo();
            BeanUtils.copyProperties(staff, staffVo);
            return staffVo;
        }).collect(Collectors.toList()));
        return vo;
    }

    private void copyFields(MerchantStoreSaveBo saveBo, MerchantStore store) {
        store.setName(saveBo.getName());
        store.setAddress(saveBo.getAddress());
        store.setProvince(saveBo.getProvince());
        store.setCity(saveBo.getCity());
        store.setDistrict(saveBo.getDistrict());
        store.setContactPhone(saveBo.getContactPhone());
        store.setBusinessHours(saveBo.getBusinessHours());
        store.setServiceArea(saveBo.getServiceArea());
        store.setLongitude(saveBo.getLongitude());
        store.setLatitude(saveBo.getLatitude());
        if (saveBo.getDeliveryEnabled() != null) {
            store.setDeliveryEnabled(saveBo.getDeliveryEnabled());
        }
        if (saveBo.getSelfPickupEnabled() != null) {
            store.setSelfPickupEnabled(saveBo.getSelfPickupEnabled());
        }
        store.setBaseDeliveryFee(saveBo.getBaseDeliveryFee());
        store.setFreeDeliveryThreshold(saveBo.getFreeDeliveryThreshold());
        if (saveBo.getStatus() != null) {
            store.setStatus(saveBo.getStatus());
        }
    }
}
