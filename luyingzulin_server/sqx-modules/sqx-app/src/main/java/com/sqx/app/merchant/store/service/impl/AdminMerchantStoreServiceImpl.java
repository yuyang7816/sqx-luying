package com.sqx.app.merchant.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStorePageBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreSaveBo;
import com.sqx.app.merchant.store.domain.bo.AdminMerchantStoreUpdateStatusBo;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.domain.vo.AdminMerchantStoreListVo;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.store.service.AdminMerchantStoreService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminMerchantStoreServiceImpl implements AdminMerchantStoreService {

    @Resource
    private MerchantStoreMapper merchantStoreMapper;
    @Resource
    private MerchantMapper merchantMapper;

    @Override
    public IPage<AdminMerchantStoreListVo> adminPage(AdminMerchantStorePageBo pageBo) {
        Page<MerchantStore> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<MerchantStore> wrapper = new LambdaQueryWrapper<>();
        if (pageBo.getMerchantId() != null) {
            wrapper.eq(MerchantStore::getMerchantId, pageBo.getMerchantId());
        }
        if (pageBo.getStatus() != null) {
            wrapper.eq(MerchantStore::getStatus, pageBo.getStatus());
        }
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(MerchantStore::getId, id);
            } else {
                wrapper.and(w -> w.like(MerchantStore::getName, pageBo.getKeyword())
                        .or().like(MerchantStore::getAddress, pageBo.getKeyword())
                        .or().like(MerchantStore::getContactPhone, pageBo.getKeyword()));
            }
        }
        wrapper.orderByDesc(MerchantStore::getCreateTime);
        IPage<MerchantStore> storePage = merchantStoreMapper.selectPage(page, wrapper);

        Set<Long> merchantIds = storePage.getRecords().stream()
                .map(MerchantStore::getMerchantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Merchant> merchantMap = merchantIds.isEmpty()
                ? Collections.emptyMap()
                : merchantMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, Function.identity()));

        List<AdminMerchantStoreListVo> records = storePage.getRecords().stream()
                .map(store -> toListVo(store, merchantMap.get(store.getMerchantId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(storePage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSave(AdminMerchantStoreSaveBo saveBo) {
        Merchant merchant = merchantMapper.selectById(saveBo.getMerchantId());
        if (merchant == null) {
            throw new BizException("所属商户不存在");
        }

        if (saveBo.getId() != null) {
            MerchantStore existing = merchantStoreMapper.selectById(saveBo.getId());
            if (existing == null || !saveBo.getMerchantId().equals(existing.getMerchantId())) {
                throw new BizException("门店不存在或不属于该商户");
            }
            copyFields(saveBo, existing);
            merchantStoreMapper.updateById(existing);
            return;
        }

        MerchantStore store = new MerchantStore();
        store.setStoreNo("STORE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        store.setMerchantId(saveBo.getMerchantId());
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdateStatus(AdminMerchantStoreUpdateStatusBo updateStatusBo) {
        MerchantStore store = merchantStoreMapper.selectById(updateStatusBo.getId());
        if (store == null) {
            throw new BizException("门店不存在");
        }
        if (updateStatusBo.getStatus() != 0 && updateStatusBo.getStatus() != 1) {
            throw new BizException("门店状态无效");
        }
        store.setStatus(updateStatusBo.getStatus());
        merchantStoreMapper.updateById(store);
    }

    private AdminMerchantStoreListVo toListVo(MerchantStore store, Merchant merchant) {
        AdminMerchantStoreListVo vo = new AdminMerchantStoreListVo();
        vo.setId(store.getId());
        vo.setStoreNo(store.getStoreNo());
        vo.setMerchantId(store.getMerchantId());
        vo.setMerchantName(merchant == null ? null : merchant.getName());
        vo.setName(store.getName());
        vo.setAddress(store.getAddress());
        vo.setContactPhone(store.getContactPhone());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setLongitude(store.getLongitude());
        vo.setLatitude(store.getLatitude());
        vo.setStatus(store.getStatus());
        vo.setCreateTime(store.getCreateTime());
        return vo;
    }

    private void copyFields(AdminMerchantStoreSaveBo saveBo, MerchantStore store) {
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
