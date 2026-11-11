package com.sqx.app.merchant.order.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.merchant.enums.MerchantOrderStatus;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.order.domain.entity.MerchantOrder;
import com.sqx.app.merchant.order.mapper.MerchantOrderMapper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户订单 ↔ 商户订单同步（LINK-{rentOrderId}）
 */
@Component
public class MerchantOrderSyncSupport {

    @Resource
    private MerchantOrderMapper merchantOrderMapper;
    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    /**
     * 支付成功后创建商户订单（幂等）
     */
    public void syncOnPaySuccess(RentOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        if (order.getMerchantId() == null) {
            return;
        }

        MerchantOrder existing = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getRentOrderId, order.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }

        MerchantOrder merchantOrder = new MerchantOrder();
        merchantOrder.setMerchantOrderNo(MerchantOrder.buildLinkNo(order.getId()));
        merchantOrder.setMerchantId(order.getMerchantId());
        merchantOrder.setStoreId(order.getStoreId() != null ? order.getStoreId() : order.getPickupStoreId());
        merchantOrder.setRentOrderId(order.getId());
        merchantOrder.setRentOrderNo(order.getOrderNo());
        merchantOrder.setUserId(order.getUserId());
        merchantOrder.setGoodsName(resolveGoodsName(order.getId()));
        merchantOrder.setStatus(MerchantOrderStatus.PENDING_PREPARE.getCode());
        merchantOrder.setContactName(order.getContactName());
        merchantOrder.setContactPhone(order.getContactPhone());
        merchantOrder.setPickupType(order.getPickupType());
        merchantOrder.setAddress(resolveAddress(order));
        merchantOrder.setPayAmount(order.getPayAmount());
        merchantOrder.setRemark(order.getRemark());
        merchantOrderMapper.insert(merchantOrder);
    }

    /**
     * 用户订单状态变更时同步商户订单
     */
    public void syncFromRentOrder(RentOrder order) {
        if (order == null || order.getId() == null) {
            return;
        }
        MerchantOrder merchantOrder = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getRentOrderId, order.getId())
                .last("LIMIT 1"));
        if (merchantOrder == null) {
            if (order.getMerchantId() != null && RentOrderStateMachineHelper.isSyncable(order.getStatus())) {
                syncOnPaySuccess(order);
                merchantOrder = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                        .eq(MerchantOrder::getRentOrderId, order.getId())
                        .last("LIMIT 1"));
            }
            if (merchantOrder == null) {
                return;
            }
        }

        String targetStatus = MerchantOrderStatus.fromRentOrderStatus(order.getStatus());
        if (MerchantOrderStatus.PENDING_PREPARE.getCode().equals(merchantOrder.getStatus())
                && MerchantOrderStatus.PENDING_PREPARE.getCode().equals(targetStatus)) {
            return;
        }
        if (MerchantOrderStatus.PREPARED.getCode().equals(merchantOrder.getStatus())
                && MerchantOrderStatus.PENDING_PREPARE.getCode().equals(targetStatus)) {
            return;
        }

        LambdaUpdateWrapper<MerchantOrder> wrapper = new LambdaUpdateWrapper<MerchantOrder>()
                .eq(MerchantOrder::getId, merchantOrder.getId())
                .set(MerchantOrder::getStatus, targetStatus);

        if (OrderStatus.RENTING.getCode().equals(order.getStatus())) {
            wrapper.set(MerchantOrder::getPickupTime, order.getPickupTime() != null
                    ? order.getPickupTime() : LocalDateTime.now());
        }
        if (OrderStatus.COMPLETED.getCode().equals(order.getStatus())) {
            wrapper.set(MerchantOrder::getCompleteTime, order.getCompleteTime() != null
                    ? order.getCompleteTime() : LocalDateTime.now());
        }
        merchantOrderMapper.update(null, wrapper);
    }

    public void markPrepared(Long merchantOrderId) {
        merchantOrderMapper.update(null, new LambdaUpdateWrapper<MerchantOrder>()
                .eq(MerchantOrder::getId, merchantOrderId)
                .eq(MerchantOrder::getStatus, MerchantOrderStatus.PENDING_PREPARE.getCode())
                .set(MerchantOrder::getStatus, MerchantOrderStatus.PREPARED.getCode())
                .set(MerchantOrder::getPrepareTime, LocalDateTime.now()));
    }

    public void markRenting(Long merchantOrderId, LocalDateTime pickupTime) {
        merchantOrderMapper.update(null, new LambdaUpdateWrapper<MerchantOrder>()
                .eq(MerchantOrder::getId, merchantOrderId)
                .in(MerchantOrder::getStatus,
                        MerchantOrderStatus.PENDING_PREPARE.getCode(),
                        MerchantOrderStatus.PREPARED.getCode())
                .set(MerchantOrder::getStatus, MerchantOrderStatus.RENTING.getCode())
                .set(MerchantOrder::getPickupTime, pickupTime));
    }

    public void markCompleted(Long rentOrderId, LocalDateTime completeTime) {
        merchantOrderMapper.update(null, new LambdaUpdateWrapper<MerchantOrder>()
                .eq(MerchantOrder::getRentOrderId, rentOrderId)
                .set(MerchantOrder::getStatus, MerchantOrderStatus.COMPLETED.getCode())
                .set(MerchantOrder::getCompleteTime, completeTime));
    }

    private String resolveGoodsName(Long orderId) {
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, orderId));
        if (CollectionUtils.isEmpty(items)) {
            return "租赁商品";
        }
        return items.stream()
                .map(RentOrderItem::getGoodsName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("、"));
    }

    private String resolveAddress(RentOrder order) {
        if (PickupType.DELIVERY.getCode().equals(order.getPickupType())) {
            return "配送订单（联系人：" + StringUtils.defaultString(order.getContactName(), "用户") + "）";
        }
        Long storeId = order.getPickupStoreId() != null ? order.getPickupStoreId() : order.getStoreId();
        if (storeId != null) {
            MerchantStore store = merchantStoreMapper.selectById(storeId);
            if (store != null) {
                return store.getAddress();
            }
        }
        return "门店自提";
    }

    /** 内部辅助，避免循环依赖 rental order support */
    static final class RentOrderStateMachineHelper {
        private RentOrderStateMachineHelper() {
        }

        static boolean isSyncable(String status) {
            return OrderStatus.WAITING_PICKUP.getCode().equals(status)
                    || OrderStatus.DELIVERING.getCode().equals(status)
                    || OrderStatus.RENTING.getCode().equals(status)
                    || OrderStatus.RETURNING.getCode().equals(status)
                    || OrderStatus.INSPECTING.getCode().equals(status)
                    || OrderStatus.COMPLETED.getCode().equals(status);
        }
    }
}
