package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.domain.entity.MerchantApply;
import com.sqx.app.merchant.apply.mapper.MerchantApplyMapper;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentPickupCode;
import com.sqx.app.rental.order.domain.vo.RentPickupCodeVo;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentPickupCodeMapper;
import com.sqx.app.rental.order.service.RentPickupService;
import com.sqx.app.rental.order.support.RentOrderMerchantSupport;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.order.support.RentOrderSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 取货服务实现
 */
@Service
public class RentPickupServiceImpl implements RentPickupService {

    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentPickupCodeMapper rentPickupCodeMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;
    @Resource
    private MerchantApplyMapper merchantApplyMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private RentOrderMerchantSupport rentOrderMerchantSupport;

    @Override
    public RentPickupCodeVo getPickupCode(Long userId, Long orderId) {
        RentOrder order = RentOrderSupport.requireOwnedOrder(rentOrderMapper, userId, orderId);
        if (StringUtils.isBlank(order.getPickupCode())) {
            throw new BizException("订单暂无取货码，请确认已支付");
        }

        RentPickupCode codeEntity = rentPickupCodeMapper.selectOne(new LambdaQueryWrapper<RentPickupCode>()
                .eq(RentPickupCode::getOrderId, orderId)
                .orderByDesc(RentPickupCode::getId)
                .last("LIMIT 1"));

        boolean codeUsed = codeEntity != null && codeEntity.getUsed() != null && codeEntity.getUsed() == 1;
        boolean verified = codeUsed
                || (!OrderStatus.WAITING_PICKUP.getCode().equals(order.getStatus())
                && !OrderStatus.DELIVERING.getCode().equals(order.getStatus())
                && !OrderStatus.PENDING_PAYMENT.getCode().equals(order.getStatus())
                && RentOrderStateMachine.isPaidStatus(order.getStatus()));

        RentPickupCodeVo vo = new RentPickupCodeVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPickupCode(order.getPickupCode());
        vo.setPickupCodeDisplay(RentOrderSupport.formatPickupCodeDisplay(order.getPickupCode()));
        vo.setUsed(codeUsed);
        vo.setVerified(verified);
        vo.setOrderStatus(order.getStatus());
        vo.setExpireTime(codeEntity == null ? order.getEndTime() : codeEntity.getExpireTime());
        vo.setPickupType(order.getPickupType());
        vo.setPickupStoreId(order.getPickupStoreId());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        fillStoreInfo(vo, order);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmPickup(Long userId, Long orderId) {
        RentOrder order = RentOrderSupport.requireOwnedOrder(rentOrderMapper, userId, orderId);
        RentOrderStateMachine.assertCanPickup(order.getStatus());
        String targetStatus = OrderStatus.RENTING.getCode();
        RentOrderStateMachine.assertTransition(order.getStatus(), targetStatus);

        LocalDateTime now = LocalDateTime.now();
        int updated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, orderId)
                .in(RentOrder::getStatus,
                        OrderStatus.WAITING_PICKUP.getCode(),
                        OrderStatus.DELIVERING.getCode())
                .set(RentOrder::getStatus, targetStatus)
                .set(RentOrder::getPickupTime, now));
        if (updated == 0) {
            throw new BizException("取货确认失败，订单状态已变更");
        }

        rentPickupCodeMapper.update(null, new LambdaUpdateWrapper<RentPickupCode>()
                .eq(RentPickupCode::getOrderId, orderId)
                .eq(RentPickupCode::getUsed, 0)
                .set(RentPickupCode::getUsed, 1)
                .set(RentPickupCode::getUseTime, now));

        RentOrderSupport.writeOrderLog(rentOrderLogMapper, orderId, order.getOrderNo(),
                order.getStatus(), targetStatus, "user", userId, "用户确认取货");

        RentOrder synced = rentOrderMapper.selectById(orderId);
        if (synced != null) {
            merchantOrderSyncSupport.syncFromRentOrder(synced);
        }
    }

    private void fillStoreInfo(RentPickupCodeVo vo, RentOrder order) {
        Long merchantId = rentOrderMerchantSupport.resolveMerchantId(order.getId(), order.getMerchantId());
        fillMerchantName(vo, merchantId);

        MerchantStore store = rentOrderMerchantSupport.resolveMerchantStore(merchantId, order.getPickupStoreId());
        if (store == null) {
            vo.setStoreName(StringUtils.defaultIfBlank(vo.getMerchantName(), "默认自提门店"));
            vo.setStoreAddress("请到店出示取货码核销");
            vo.setFullAddress(vo.getStoreAddress());
            return;
        }

        vo.setPickupStoreId(store.getId());
        vo.setStoreName(store.getName());
        vo.setStoreAddress(store.getAddress());
        vo.setStorePhone(store.getContactPhone());
        vo.setProvince(store.getProvince());
        vo.setCity(store.getCity());
        vo.setDistrict(store.getDistrict());
        vo.setFullAddress(buildFullAddress(store));
        vo.setLatitude(store.getLatitude());
        vo.setLongitude(store.getLongitude());
        vo.setBusinessHours(store.getBusinessHours());
        vo.setFacadeImage(resolveFacadeImage(store.getId()));
    }

    private void fillMerchantName(RentPickupCodeVo vo, Long merchantId) {
        if (merchantId == null) {
            return;
        }
        vo.setMerchantId(merchantId);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant != null && StringUtils.isNotBlank(merchant.getName())) {
            vo.setMerchantName(merchant.getName());
        }
    }

    private String buildFullAddress(MerchantStore store) {
        StringBuilder sb = new StringBuilder();
        appendAddressPart(sb, store.getProvince());
        appendAddressPart(sb, store.getCity());
        appendAddressPart(sb, store.getDistrict());
        appendAddressPart(sb, store.getAddress());
        if (sb.length() == 0) {
            return StringUtils.defaultString(store.getAddress());
        }
        return sb.toString().trim();
    }

    private void appendAddressPart(StringBuilder sb, String part) {
        if (StringUtils.isBlank(part)) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(part.trim());
    }

    private String resolveFacadeImage(Long storeId) {
        MerchantApply apply = merchantApplyMapper.selectOne(new LambdaQueryWrapper<MerchantApply>()
                .eq(MerchantApply::getStoreId, storeId)
                .eq(MerchantApply::getStatus, "approved")
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));
        if (apply == null || StringUtils.isBlank(apply.getExtraUrls())) {
            return null;
        }
        String[] urls = apply.getExtraUrls().split(",");
        return urls.length > 0 ? urls[0].trim() : null;
    }
}
