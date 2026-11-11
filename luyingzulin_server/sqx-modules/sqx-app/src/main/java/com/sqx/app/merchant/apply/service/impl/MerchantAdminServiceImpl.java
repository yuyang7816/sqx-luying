package com.sqx.app.merchant.apply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.merchant.apply.domain.bo.MerchantPageBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantUpdateStatusBo;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.domain.vo.MerchantDetailVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantListVo;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.apply.service.MerchantAdminService;
import com.sqx.app.merchant.enums.MerchantStatus;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.domain.vo.AdminMerchantStoreBriefVo;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.wallet.domain.entity.MerchantDeposit;
import com.sqx.app.merchant.wallet.mapper.MerchantDepositMapper;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MerchantAdminServiceImpl implements MerchantAdminService {

    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;
    @Resource
    private MerchantDepositMapper merchantDepositMapper;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;

    @Override
    public IPage<MerchantListVo> page(MerchantPageBo pageBo) {
        Page<Merchant> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(Merchant::getId, id);
            } else {
                wrapper.and(w -> w.like(Merchant::getName, pageBo.getKeyword())
                        .or().like(Merchant::getContactName, pageBo.getKeyword())
                        .or().like(Merchant::getContactPhone, pageBo.getKeyword()));
            }
        }
        if (StringUtils.isNotBlank(pageBo.getStatus())) {
            wrapper.eq(Merchant::getStatus, pageBo.getStatus());
        }
        wrapper.orderByDesc(Merchant::getCreateTime);
        IPage<Merchant> merchantPage = merchantMapper.selectPage(page, wrapper);
        BigDecimal requiredAmount = merchantSecurityDepositService.getRequiredAmount();
        Map<Long, MerchantDeposit> depositMap = loadDepositMap(merchantPage.getRecords());
        List<MerchantListVo> records = merchantPage.getRecords().stream()
                .map(merchant -> toListVo(merchant, depositMap, requiredAmount))
                .collect(Collectors.toList());
        return PageUtil.copyPage(merchantPage, records);
    }

    @Override
    public MerchantDetailVo detail(Long id) {
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BizException("商户不存在");
        }
        MerchantDetailVo vo = new MerchantDetailVo();
        vo.setId(merchant.getId());
        vo.setMerchantNo(merchant.getMerchantNo());
        vo.setName(merchant.getName());
        vo.setContactName(merchant.getContactName());
        vo.setContactPhone(merchant.getContactPhone());
        vo.setUserId(merchant.getUserId());
        vo.setStatus(merchant.getStatus());
        vo.setStatusLabel(MerchantStatus.fromCode(merchant.getStatus()).getDesc());
        vo.setCreateTime(merchant.getCreateTime());

        List<MerchantStore> stores = merchantStoreMapper.selectList(new LambdaQueryWrapper<MerchantStore>()
                .eq(MerchantStore::getMerchantId, merchant.getId())
                .orderByAsc(MerchantStore::getId));
        vo.setStores(stores.stream().map(store -> {
            AdminMerchantStoreBriefVo brief = new AdminMerchantStoreBriefVo();
            brief.setId(store.getId());
            brief.setStoreNo(store.getStoreNo());
            brief.setName(store.getName());
            brief.setAddress(store.getAddress());
            brief.setStatus(store.getStatus());
            return brief;
        }).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(MerchantUpdateStatusBo updateStatusBo) {
        Merchant merchant = merchantMapper.selectById(updateStatusBo.getId());
        if (merchant == null) {
            throw new BizException("商户不存在");
        }
        MerchantStatus status = MerchantStatus.fromCode(updateStatusBo.getStatus());
        merchant.setStatus(status.getCode());
        merchantMapper.updateById(merchant);
    }

    private Map<Long, MerchantDeposit> loadDepositMap(List<Merchant> merchants) {
        if (merchants == null || merchants.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> merchantIds = merchants.stream().map(Merchant::getId).collect(Collectors.toList());
        List<MerchantDeposit> deposits = merchantDepositMapper.selectList(new LambdaQueryWrapper<MerchantDeposit>()
                .in(MerchantDeposit::getMerchantId, merchantIds));
        return deposits.stream().collect(Collectors.toMap(MerchantDeposit::getMerchantId, item -> item, (a, b) -> a));
    }

    private MerchantListVo toListVo(Merchant merchant, Map<Long, MerchantDeposit> depositMap, BigDecimal requiredAmount) {
        MerchantListVo vo = new MerchantListVo();
        vo.setId(merchant.getId());
        vo.setMerchantNo(merchant.getMerchantNo());
        vo.setName(merchant.getName());
        vo.setContactName(merchant.getContactName());
        vo.setContactPhone(merchant.getContactPhone());
        vo.setUserId(merchant.getUserId());
        vo.setStatus(merchant.getStatus());
        vo.setStatusLabel(MerchantStatus.fromCode(merchant.getStatus()).getDesc());
        vo.setCreateTime(merchant.getCreateTime());

        MerchantDeposit deposit = depositMap.get(merchant.getId());
        BigDecimal totalAmount = deposit == null || deposit.getTotalAmount() == null
                ? BigDecimal.ZERO : deposit.getTotalAmount();
        vo.setSecurityDepositAmount(totalAmount);
        vo.setSecurityDepositRequiredAmount(requiredAmount);
        if (requiredAmount == null || requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setSecurityDepositPaid(true);
            vo.setSecurityDepositStatusLabel("无需缴纳");
        } else if (totalAmount.compareTo(requiredAmount) >= 0) {
            vo.setSecurityDepositPaid(true);
            vo.setSecurityDepositStatusLabel("已缴足");
        } else {
            vo.setSecurityDepositPaid(false);
            vo.setSecurityDepositStatusLabel("未缴足");
        }
        return vo;
    }
}
