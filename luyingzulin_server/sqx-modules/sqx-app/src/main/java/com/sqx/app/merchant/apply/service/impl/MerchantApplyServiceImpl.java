package com.sqx.app.merchant.apply.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyAuditBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyPageBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplySubmitBo;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.domain.entity.MerchantApply;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyListVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyProgressVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyTimelineVo;
import com.sqx.app.merchant.apply.mapper.MerchantApplyMapper;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.apply.service.MerchantApplyService;
import com.sqx.app.merchant.enums.ApplyStatus;
import com.sqx.app.merchant.enums.MerchantStatus;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MerchantApplyServiceImpl implements MerchantApplyService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private MerchantApplyMapper merchantApplyMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, MerchantApplySubmitBo submitBo) {
        Merchant existingMerchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (existingMerchant != null) {
            throw new BizException("您已是商户，无需重复申请");
        }

        MerchantApply reviewing = merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .in(MerchantApply::getStatus, ApplyStatus.PENDING.getCode(), ApplyStatus.REVIEWING.getCode())
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));
        if (reviewing != null) {
            throw new BizException("您已有进行中的入驻申请，请等待审核");
        }

        MerchantApply rejected = merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .eq(MerchantApply::getStatus, ApplyStatus.REJECTED.getCode())
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));

        MerchantApply apply;
        if (rejected != null) {
            apply = rejected;
            apply.setRejectReason(null);
            apply.setAuditAdminId(null);
            apply.setAuditTime(null);
        } else {
            apply = new MerchantApply();
            apply.setApplyNo("APPLY-" + System.currentTimeMillis());
            apply.setUserId(userId);
        }

        fillApplyFromSubmitBo(apply, submitBo);
        apply.setStatus(ApplyStatus.REVIEWING.getCode());
        if (rejected != null) {
            merchantApplyMapper.updateById(apply);
        } else {
            merchantApplyMapper.insert(apply);
        }
    }

    @Override
    public MerchantApplyProgressVo progress(Long userId) {
        Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, userId)
                .last("LIMIT 1"));
        if (merchant != null) {
            MerchantStore store = merchantStoreMapper.selectOne(new LambdaQueryWrapper<MerchantStore>()
                    .eq(MerchantStore::getMerchantId, merchant.getId())
                    .orderByAsc(MerchantStore::getId)
                    .last("LIMIT 1"));
            MerchantApplyProgressVo vo = new MerchantApplyProgressVo();
            vo.setStatus(ApplyStatus.APPROVED.getCode());
            vo.setStatusLabel(ApplyStatus.APPROVED.getDesc());
            vo.setMerchantName(merchant.getName());
            vo.setStoreName(store == null ? null : store.getName());
            vo.setContactName(merchant.getContactName());
            vo.setContactPhone(merchant.getContactPhone());
            vo.setStoreAddress(store == null ? null : store.getAddress());
            vo.setMerchantId(merchant.getId());
            vo.setStoreId(store == null ? null : store.getId());
            vo.setTimeline(buildApprovedTimeline(merchant));
            return vo;
        }

        MerchantApply apply = merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getUserId, userId)
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));
        if (apply == null) {
            MerchantApplyProgressVo vo = new MerchantApplyProgressVo();
            vo.setStatus(ApplyStatus.PENDING.getCode());
            vo.setStatusLabel("未提交");
            vo.setTimeline(Collections.emptyList());
            return vo;
        }

        MerchantApplyProgressVo vo = new MerchantApplyProgressVo();
        vo.setApplyId(apply.getId());
        vo.setApplyNo(apply.getApplyNo());
        vo.setStatus(apply.getStatus());
        vo.setStatusLabel(ApplyStatus.fromCode(apply.getStatus()).getDesc());
        vo.setMerchantName(resolveStoreDisplayName(apply));
        vo.setStoreName(resolveStoreDisplayName(apply));
        vo.setContactName(apply.getContactName());
        vo.setContactPhone(apply.getContactPhone());
        vo.setStoreAddress(apply.getStoreAddress());
        vo.setBusinessHours(apply.getBusinessHours());
        vo.setServiceArea(apply.getServiceArea());
        vo.setProvince(apply.getProvince());
        vo.setCity(apply.getCity());
        vo.setDistrict(apply.getDistrict());
        vo.setLatitude(apply.getLatitude());
        vo.setLongitude(apply.getLongitude());
        vo.setBaseDeliveryFee(apply.getBaseDeliveryFee());
        vo.setFreeDeliveryThreshold(apply.getFreeDeliveryThreshold());
        vo.setBusinessScopes(splitList(apply.getBusinessScope()));
        vo.setLicenseUrls(splitList(apply.getLicenseUrls()));
        vo.setExtraUrls(splitList(apply.getExtraUrls()));
        vo.setRejectReason(apply.getRejectReason());
        vo.setMerchantId(apply.getMerchantId());
        vo.setStoreId(apply.getStoreId());
        vo.setTimeline(buildTimeline(apply));
        return vo;
    }

    @Override
    public IPage<MerchantApplyListVo> adminPage(MerchantApplyPageBo pageBo) {
        Page<MerchantApply> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<MerchantApply> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(MerchantApply::getId, id);
            } else {
                wrapper.and(w -> w.like(MerchantApply::getMerchantName, pageBo.getKeyword())
                        .or().like(MerchantApply::getStoreName, pageBo.getKeyword())
                        .or().like(MerchantApply::getContactName, pageBo.getKeyword())
                        .or().like(MerchantApply::getContactPhone, pageBo.getKeyword()));
            }
        }
        if (StringUtils.isNotBlank(pageBo.getStatus())) {
            wrapper.eq(MerchantApply::getStatus, pageBo.getStatus());
        }
        wrapper.orderByDesc(MerchantApply::getCreateTime);
        IPage<MerchantApply> applyPage = merchantApplyMapper.selectPage(page, wrapper);
        List<MerchantApplyListVo> records = applyPage.getRecords().stream()
                .map(this::toListVo).collect(Collectors.toList());
        return PageUtil.copyPage(applyPage, records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long adminId, MerchantApplyAuditBo auditBo) {
        MerchantApply apply = merchantApplyMapper.selectById(auditBo.getApplyId());
        if (apply == null) {
            throw new BizException("入驻申请不存在");
        }
        if (!ApplyStatus.REVIEWING.getCode().equals(apply.getStatus())) {
            throw new BizException("仅审核中申请可操作");
        }

        String result = auditBo.getAuditResult();
        if ("rejected".equals(result)) {
            if (StringUtils.isBlank(auditBo.getRejectReason())) {
                throw new BizException("驳回时必须填写原因");
            }
            apply.setStatus(ApplyStatus.REJECTED.getCode());
            apply.setRejectReason(auditBo.getRejectReason());
            apply.setAuditAdminId(adminId);
            apply.setAuditTime(LocalDateTime.now());
            merchantApplyMapper.updateById(apply);
            return;
        }

        if (!"approved".equals(result)) {
            throw new BizException("审核结果无效");
        }

        Merchant existing = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, apply.getUserId())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new BizException("该用户已是商户");
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantNo("MCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        merchant.setUserId(apply.getUserId());
        merchant.setName(resolveStoreDisplayName(apply));
        merchant.setContactName(apply.getContactName());
        merchant.setContactPhone(apply.getContactPhone());
        merchant.setStatus(MerchantStatus.ACTIVE.getCode());
        merchantMapper.insert(merchant);

        MerchantStore store = new MerchantStore();
        store.setStoreNo("STORE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        store.setMerchantId(merchant.getId());
        store.setName(resolveStoreDisplayName(apply));
        store.setAddress(apply.getStoreAddress());
        store.setProvince(apply.getProvince());
        store.setCity(apply.getCity());
        store.setDistrict(apply.getDistrict());
        store.setLatitude(apply.getLatitude());
        store.setLongitude(apply.getLongitude());
        store.setContactPhone(apply.getContactPhone());
        store.setBusinessHours(StringUtils.defaultIfBlank(apply.getBusinessHours(), "09:00-21:00"));
        store.setServiceArea(apply.getServiceArea());
        store.setBaseDeliveryFee(apply.getBaseDeliveryFee());
        store.setFreeDeliveryThreshold(apply.getFreeDeliveryThreshold());
        store.setStatus(1);
        store.setDeliveryEnabled(1);
        store.setSelfPickupEnabled(1);
        merchantStoreMapper.insert(store);

        apply.setStatus(ApplyStatus.APPROVED.getCode());
        apply.setMerchantId(merchant.getId());
        apply.setStoreId(store.getId());
        apply.setAuditAdminId(adminId);
        apply.setAuditTime(LocalDateTime.now());
        merchantApplyMapper.updateById(apply);
    }

    private MerchantApplyListVo toListVo(MerchantApply apply) {
        MerchantApplyListVo vo = new MerchantApplyListVo();
        vo.setId(apply.getId());
        vo.setApplyNo(apply.getApplyNo());
        vo.setMerchantName(resolveStoreDisplayName(apply));
        vo.setStoreName(resolveStoreDisplayName(apply));
        vo.setContactName(apply.getContactName());
        vo.setContactPhone(apply.getContactPhone());
        vo.setStoreAddress(apply.getStoreAddress());
        vo.setBusinessHours(apply.getBusinessHours());
        vo.setServiceArea(apply.getServiceArea());
        vo.setProvince(apply.getProvince());
        vo.setCity(apply.getCity());
        vo.setDistrict(apply.getDistrict());
        vo.setLatitude(apply.getLatitude());
        vo.setLongitude(apply.getLongitude());
        vo.setBaseDeliveryFee(apply.getBaseDeliveryFee());
        vo.setFreeDeliveryThreshold(apply.getFreeDeliveryThreshold());
        vo.setBusinessScopes(splitList(apply.getBusinessScope()));
        vo.setLicenseUrls(splitList(apply.getLicenseUrls()));
        vo.setExtraUrls(splitList(apply.getExtraUrls()));
        vo.setStatus(apply.getStatus());
        vo.setStatusLabel(ApplyStatus.fromCode(apply.getStatus()).getDesc());
        vo.setRejectReason(apply.getRejectReason());
        vo.setCreateTime(apply.getCreateTime());
        vo.setAuditTime(apply.getAuditTime());
        return vo;
    }

    private void fillApplyFromSubmitBo(MerchantApply apply, MerchantApplySubmitBo submitBo) {
        String storeName = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(submitBo.getStoreName()),
                StringUtils.trimToNull(submitBo.getMerchantName()));
        if (StringUtils.isBlank(storeName)) {
            throw new BizException("门店名称不能为空");
        }
        apply.setStoreName(storeName);
        apply.setMerchantName(storeName);
        apply.setContactName(submitBo.getContactName());
        apply.setContactPhone(submitBo.getContactPhone());
        apply.setStoreAddress(submitBo.getStoreAddress());
        apply.setBusinessHours(submitBo.getBusinessHours());
        apply.setServiceArea(submitBo.getServiceArea());
        apply.setProvince(submitBo.getProvince());
        apply.setCity(submitBo.getCity());
        apply.setDistrict(submitBo.getDistrict());
        apply.setLatitude(submitBo.getLatitude());
        apply.setLongitude(submitBo.getLongitude());
        apply.setBaseDeliveryFee(submitBo.getBaseDeliveryFee());
        apply.setFreeDeliveryThreshold(submitBo.getFreeDeliveryThreshold());
        apply.setBusinessScope(joinList(submitBo.getBusinessScopes()));
        apply.setLicenseUrls(joinList(submitBo.getLicenseUrls()));
        apply.setExtraUrls(joinList(submitBo.getExtraUrls()));
    }

    private String resolveStoreDisplayName(MerchantApply apply) {
        return StringUtils.defaultIfBlank(apply.getStoreName(), apply.getMerchantName());
    }

    private List<MerchantApplyTimelineVo> buildTimeline(MerchantApply apply) {
        List<MerchantApplyTimelineVo> list = new ArrayList<>();
        MerchantApplyTimelineVo submit = new MerchantApplyTimelineVo();
        submit.setTitle("提交入驻申请");
        submit.setTime(formatTime(apply.getCreateTime()));
        submit.setDesc("已提交商家基本信息与门店地址");
        list.add(submit);

        if (ApplyStatus.REVIEWING.getCode().equals(apply.getStatus())) {
            MerchantApplyTimelineVo review = new MerchantApplyTimelineVo();
            review.setTitle("平台审核");
            review.setTime("进行中");
            review.setDesc("平台正在核验资质与经营信息");
            list.add(review);
        } else if (ApplyStatus.REJECTED.getCode().equals(apply.getStatus())) {
            MerchantApplyTimelineVo reject = new MerchantApplyTimelineVo();
            reject.setTitle("审核驳回");
            reject.setTime(formatTime(apply.getAuditTime()));
            reject.setDesc(StringUtils.defaultIfBlank(apply.getRejectReason(), "请修改资料后重新提交"));
            list.add(reject);
        } else if (ApplyStatus.APPROVED.getCode().equals(apply.getStatus())) {
            MerchantApplyTimelineVo pass = new MerchantApplyTimelineVo();
            pass.setTitle("审核通过");
            pass.setTime(formatTime(apply.getAuditTime()));
            pass.setDesc("已开通商户权限，可进入工作台");
            list.add(pass);
        }
        return list;
    }

    private List<MerchantApplyTimelineVo> buildApprovedTimeline(Merchant merchant) {
        MerchantApplyTimelineVo pass = new MerchantApplyTimelineVo();
        pass.setTitle("已开通经营");
        pass.setTime(formatTime(merchant.getCreateTime()));
        pass.setDesc("商户「" + merchant.getName() + "」已启用");
        return Collections.singletonList(pass);
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "—" : time.format(DT_FMT);
    }

    private String joinList(List<String> items) {
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        return String.join(",", items);
    }

    private List<String> splitList(String raw) {
        if (StringUtils.isBlank(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(",")).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }
}
