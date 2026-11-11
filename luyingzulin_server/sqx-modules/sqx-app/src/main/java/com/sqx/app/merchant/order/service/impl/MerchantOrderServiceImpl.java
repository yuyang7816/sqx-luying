package com.sqx.app.merchant.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.merchant.enums.MerchantOrderStatus;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPageBo;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPickupBo;
import com.sqx.app.merchant.order.domain.entity.MerchantOrder;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderContactVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderDetailVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderGoodsItemVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderListVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderTimelineVo;
import com.sqx.app.merchant.order.mapper.MerchantOrderMapper;
import com.sqx.app.merchant.order.service.MerchantOrderService;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.order.domain.entity.RentInspection;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.domain.entity.RentPickupCode;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.mapper.RentInspectionMapper;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentPickupCodeMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.app.rental.order.service.impl.RentInspectionServiceImpl;
import com.sqx.app.rental.order.service.impl.RentReturnServiceImpl;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.order.support.RentOrderSupport;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private MerchantOrderMapper merchantOrderMapper;
    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private RentPickupCodeMapper rentPickupCodeMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Resource
    private RentInspectionMapper rentInspectionMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Resource
    private com.sqx.app.rental.order.support.RentOrderNotificationSupport rentOrderNotificationSupport;
    @Resource
    private com.sqx.app.base.im.service.ImService imService;

    @Override
    public IPage<MerchantOrderListVo> page(Long userId, MerchantOrderPageBo pageBo) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        Page<MerchantOrder> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());

        LambdaQueryWrapper<MerchantOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantOrder::getMerchantId, ctx.getMerchantId());
        applyTabFilter(wrapper, pageBo.getTab());
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            wrapper.and(w -> w.like(MerchantOrder::getRentOrderNo, pageBo.getKeyword())
                    .or().like(MerchantOrder::getGoodsName, pageBo.getKeyword())
                    .or().like(MerchantOrder::getContactName, pageBo.getKeyword())
                    .or().like(MerchantOrder::getContactPhone, pageBo.getKeyword())
                    .or().like(MerchantOrder::getMerchantOrderNo, pageBo.getKeyword()));
        }
        if (StringUtils.isNotBlank(pageBo.getMerchantOrderNo())) {
            wrapper.eq(MerchantOrder::getMerchantOrderNo, pageBo.getMerchantOrderNo());
        }
        if (StringUtils.isNotBlank(pageBo.getRentOrderNo())) {
            wrapper.eq(MerchantOrder::getRentOrderNo, pageBo.getRentOrderNo());
        }
        wrapper.orderByDesc(MerchantOrder::getCreateTime);

        IPage<MerchantOrder> orderPage = merchantOrderMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(orderPage.getRecords())) {
            return PageUtil.copyPage(orderPage, Collections.emptyList());
        }

        Map<Long, UserEntity> userMap = loadUserMap(orderPage.getRecords());
        Map<Long, RentOrderContext> rentContextMap = loadRentOrderContextMap(orderPage.getRecords());
        List<MerchantOrderListVo> voList = orderPage.getRecords().stream()
                .map(o -> toListVo(o, userMap.get(o.getUserId()), rentContextMap.get(o.getRentOrderId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(orderPage, voList);
    }

    @Override
    public MerchantOrderDetailVo detail(Long userId, String id) {
        MerchantOrder order = requireOwnedOrder(userId, id);
        RentOrder rentOrder = rentOrderMapper.selectById(order.getRentOrderId());
        UserEntity user = userMapper.selectById(order.getUserId());
        RentReturnApply returnApply = null;
        RentInspection inspection = null;
        if (rentOrder != null) {
            returnApply = rentReturnApplyMapper.selectOne(new LambdaQueryWrapper<RentReturnApply>()
                    .eq(RentReturnApply::getOrderId, rentOrder.getId())
                    .orderByDesc(RentReturnApply::getId)
                    .last("LIMIT 1"));
            inspection = rentInspectionMapper.selectOne(new LambdaQueryWrapper<RentInspection>()
                    .eq(RentInspection::getOrderId, rentOrder.getId())
                    .orderByDesc(RentInspection::getId)
                    .last("LIMIT 1"));
        }
        return toDetailVo(order, rentOrder, user, returnApply, inspection, loadRentOrderItems(order.getRentOrderId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void prepare(Long userId, String id) {
        MerchantOrder order = requireOwnedOrder(userId, id);
        if (!MerchantOrderStatus.PENDING_PREPARE.getCode().equals(order.getStatus())) {
            throw new BizException("仅待备货订单可确认备货");
        }
        int updated = merchantOrderMapper.update(null, new LambdaUpdateWrapper<MerchantOrder>()
                .eq(MerchantOrder::getId, order.getId())
                .eq(MerchantOrder::getStatus, MerchantOrderStatus.PENDING_PREPARE.getCode())
                .set(MerchantOrder::getStatus, MerchantOrderStatus.PREPARED.getCode())
                .set(MerchantOrder::getPrepareTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException("备货确认失败，订单状态已变更");
        }
        RentOrder rentOrder = rentOrderMapper.selectById(order.getRentOrderId());
        if (rentOrder != null) {
            rentOrderNotificationSupport.onPrepared(rentOrder);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickup(Long userId, String id, MerchantOrderPickupBo pickupBo) {
        MerchantOrder order = requireOwnedOrder(userId, id);
        RentOrder rentOrder = requireRentOrderForPickup(order);
        assertPickupCode(rentOrder, pickupBo == null ? null : pickupBo.getPickupCode());
        doPickup(order, rentOrder, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderDetailVo pickupByCode(Long userId, MerchantOrderPickupBo pickupBo) {
        String normalized = RentOrderSupport.normalizePickupCode(pickupBo == null ? null : pickupBo.getPickupCode());
        if (StringUtils.isBlank(normalized)) {
            throw new BizException("请输入提货码");
        }
        RentOrder rentOrder = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getPickupCode, normalized)
                .last("LIMIT 1"));
        if (rentOrder == null) {
            throw new BizException("未找到对应订单，请核对提货码");
        }
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        MerchantOrder order = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getRentOrderId, rentOrder.getId())
                .eq(MerchantOrder::getMerchantId, ctx.getMerchantId())
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("该提货码不属于当前商户或不可核销");
        }
        assertPickupCode(rentOrder, normalized);
        doPickup(order, rentOrder, userId);
        return detail(userId, order.getMerchantOrderNo());
    }

    private RentOrder requireRentOrderForPickup(MerchantOrder order) {
        RentOrder rentOrder = rentOrderMapper.selectById(order.getRentOrderId());
        if (rentOrder == null) {
            throw new BizException("关联用户订单不存在");
        }
        return rentOrder;
    }

    private void assertPickupCode(RentOrder rentOrder, String inputCode) {
        String normalizedInput = RentOrderSupport.normalizePickupCode(inputCode);
        if (StringUtils.isBlank(normalizedInput)) {
            throw new BizException("请输入提货码");
        }
        String expected = RentOrderSupport.normalizePickupCode(rentOrder.getPickupCode());
        if (StringUtils.isBlank(expected)) {
            throw new BizException("订单暂无提货码，无法核销");
        }
        if (!expected.equals(normalizedInput)) {
            throw new BizException("提货码不正确，请核对后重试");
        }
    }

    private void doPickup(MerchantOrder order, RentOrder rentOrder, Long userId) {
        RentOrderStateMachine.assertCanPickup(rentOrder.getStatus());

        String targetStatus = OrderStatus.RENTING.getCode();
        LocalDateTime now = LocalDateTime.now();
        int rentUpdated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, rentOrder.getId())
                .in(RentOrder::getStatus,
                        OrderStatus.WAITING_PICKUP.getCode(),
                        OrderStatus.DELIVERING.getCode())
                .set(RentOrder::getStatus, targetStatus)
                .set(RentOrder::getPickupTime, now));
        if (rentUpdated == 0) {
            throw new BizException("核销失败，用户订单状态已变更");
        }

        rentPickupCodeMapper.update(null, new LambdaUpdateWrapper<RentPickupCode>()
                .eq(RentPickupCode::getOrderId, rentOrder.getId())
                .eq(RentPickupCode::getUsed, 0)
                .set(RentPickupCode::getUsed, 1)
                .set(RentPickupCode::getUseTime, now));

        RentOrderSupport.writeOrderLog(rentOrderLogMapper, rentOrder.getId(), rentOrder.getOrderNo(),
                rentOrder.getStatus(), targetStatus, "merchant", userId, "商户扫码/输入提货码核销");

        merchantOrderSyncSupport.markRenting(order.getId(), now);
        RentOrder picked = rentOrderMapper.selectById(rentOrder.getId());
        if (picked != null) {
            rentOrderNotificationSupport.onPickup(picked);
            imService.sendTutorialVideoAfterPickup(picked);
        }
    }

    @Override
    public MerchantOrderContactVo contact(Long userId, String id) {
        MerchantOrder order = requireOwnedOrder(userId, id);
        UserEntity user = userMapper.selectById(order.getUserId());
        MerchantOrderContactVo vo = new MerchantOrderContactVo();
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setUserId(order.getUserId());
        vo.setUserName(user == null ? order.getContactName() : user.getUserName());
        return vo;
    }

    private MerchantOrder requireOwnedOrder(Long userId, String id) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        MerchantOrder order = resolveOrder(id);
        if (!ctx.getMerchantId().equals(order.getMerchantId())) {
            throw new BizException("无权操作该订单");
        }
        return order;
    }

    private MerchantOrder resolveOrder(String id) {
        if (StringUtils.isBlank(id)) {
            throw new BizException("订单标识不能为空");
        }
        if (id.startsWith("LINK-")) {
            MerchantOrder byNo = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                    .eq(MerchantOrder::getMerchantOrderNo, id)
                    .last("LIMIT 1"));
            if (byNo != null) {
                return byNo;
            }
            try {
                Long rentOrderId = Long.parseLong(id.substring(5));
                MerchantOrder byRent = merchantOrderMapper.selectOne(new LambdaQueryWrapper<MerchantOrder>()
                        .eq(MerchantOrder::getRentOrderId, rentOrderId)
                        .last("LIMIT 1"));
                if (byRent != null) {
                    return byRent;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
            throw new BizException("商户订单不存在");
        }
        try {
            Long orderId = Long.parseLong(id);
            MerchantOrder order = merchantOrderMapper.selectById(orderId);
            if (order == null) {
                throw new BizException("商户订单不存在");
            }
            return order;
        } catch (NumberFormatException e) {
            throw new BizException("订单标识无效");
        }
    }

    private void applyTabFilter(LambdaQueryWrapper<MerchantOrder> wrapper, String tab) {
        if (StringUtils.isBlank(tab) || "all".equals(tab)) {
            return;
        }
        if ("pending".equals(tab)) {
            wrapper.in(MerchantOrder::getStatus,
                    MerchantOrderStatus.PENDING_PREPARE.getCode(),
                    MerchantOrderStatus.PREPARED.getCode());
        } else if ("running".equals(tab)) {
            wrapper.eq(MerchantOrder::getStatus, MerchantOrderStatus.RENTING.getCode());
        } else if ("review".equals(tab)) {
            wrapper.in(MerchantOrder::getStatus,
                    MerchantOrderStatus.RETURNING.getCode(),
                    MerchantOrderStatus.INSPECTING.getCode());
        } else if ("completed".equals(tab)) {
            wrapper.eq(MerchantOrder::getStatus, MerchantOrderStatus.COMPLETED.getCode());
        }
    }

    private Map<Long, UserEntity> loadUserMap(List<MerchantOrder> orders) {
        List<Long> userIds = orders.stream().map(MerchantOrder::getUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
    }

    private MerchantOrderListVo toListVo(MerchantOrder order, UserEntity user, RentOrderContext rentContext) {
        MerchantOrderStatus status = MerchantOrderStatus.fromCode(order.getStatus());
        MerchantOrderListVo vo = new MerchantOrderListVo();
        vo.setId(order.getId());
        vo.setMerchantOrderNo(order.getMerchantOrderNo());
        vo.setRentOrderId(order.getRentOrderId());
        vo.setRentOrderNo(order.getRentOrderNo());
        vo.setUserName(user == null ? order.getContactName() : user.getUserName());
        vo.setGoodsName(order.getGoodsName());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(status.getDesc());
        vo.setStatusType(status.getTabKey());
        vo.setPayAmount(order.getPayAmount());
        vo.setContactPhone(order.getContactPhone());
        vo.setPickupType(order.getPickupType());
        vo.setPickupTypeLabel(resolvePickupLabel(order.getPickupType()));
        vo.setAddress(order.getAddress());
        vo.setCreateTime(order.getCreateTime());
        vo.setNextAction(resolveNextAction(order.getStatus()));
        fillRentGoodsFields(vo, rentContext);
        return vo;
    }

    private MerchantOrderDetailVo toDetailVo(MerchantOrder order, RentOrder rentOrder, UserEntity user,
                                             RentReturnApply returnApply, RentInspection inspection,
                                             List<RentOrderItem> rentItems) {
        MerchantOrderStatus status = MerchantOrderStatus.fromCode(order.getStatus());
        MerchantOrderDetailVo vo = new MerchantOrderDetailVo();
        vo.setId(order.getId());
        vo.setMerchantOrderNo(order.getMerchantOrderNo());
        vo.setRentOrderId(order.getRentOrderId());
        vo.setRentOrderNo(order.getRentOrderNo());
        if (rentOrder != null) {
            vo.setRentOrderStatus(rentOrder.getStatus());
            vo.setRentOrderStatusLabel(OrderStatus.fromCode(rentOrder.getStatus()).getDesc());
            vo.setDepositAmount(rentOrder.getDepositAmount());
        }
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(status.getDesc());
        vo.setStatusType(status.getTabKey());
        vo.setUserName(user == null ? order.getContactName() : user.getUserName());
        vo.setGoodsName(order.getGoodsName());
        vo.setPayAmount(order.getPayAmount());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setPickupType(order.getPickupType());
        vo.setPickupTypeLabel(resolvePickupLabel(order.getPickupType()));
        vo.setAddress(order.getAddress());
        vo.setRemark(order.getRemark());
        vo.setPrepareTime(order.getPrepareTime());
        vo.setPickupTime(order.getPickupTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setNextAction(resolveNextAction(order.getStatus()));
        vo.setTimeline(buildTimeline(order, rentOrder));
        if (returnApply != null) {
            vo.setReturnDate(returnApply.getReturnDate());
            vo.setReturnMethod(returnApply.getReturnMethod());
            vo.setReturnRemark(returnApply.getRemark());
            vo.setReturnImageUrls(RentReturnServiceImpl.splitImages(returnApply.getImageUrls()));
            try {
                vo.setReturnMethodLabel(PickupType.fromCode(returnApply.getReturnMethod()).getDesc());
            } catch (BizException ignored) {
                vo.setReturnMethodLabel(returnApply.getReturnMethod());
            }
        }
        if (inspection != null) {
            vo.setInspectionRemark(inspection.getRemark());
            vo.setInspectionImageUrls(RentInspectionServiceImpl.splitImages(inspection.getImageUrls()));
            vo.setDeductAmount(inspection.getDeductAmount());
            vo.setRefundAmount(inspection.getRefundAmount());
        }
        fillRentGoodsFields(vo, new RentOrderContext(rentOrder, rentItems));
        return vo;
    }

    private Map<Long, RentOrderContext> loadRentOrderContextMap(List<MerchantOrder> orders) {
        List<Long> rentOrderIds = orders.stream()
                .map(MerchantOrder::getRentOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(rentOrderIds)) {
            return Collections.emptyMap();
        }
        Map<Long, RentOrder> rentOrderMap = rentOrderMapper.selectBatchIds(rentOrderIds).stream()
                .collect(Collectors.toMap(RentOrder::getId, o -> o, (a, b) -> a));
        Map<Long, List<RentOrderItem>> itemMap = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                        .in(RentOrderItem::getOrderId, rentOrderIds))
                .stream()
                .collect(Collectors.groupingBy(RentOrderItem::getOrderId));
        Map<Long, RentOrderContext> result = new HashMap<>();
        for (Long rentOrderId : rentOrderIds) {
            result.put(rentOrderId, new RentOrderContext(
                    rentOrderMap.get(rentOrderId),
                    itemMap.getOrDefault(rentOrderId, Collections.emptyList())));
        }
        return result;
    }

    private List<RentOrderItem> loadRentOrderItems(Long rentOrderId) {
        if (rentOrderId == null) {
            return Collections.emptyList();
        }
        return rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, rentOrderId));
    }

    private void fillRentGoodsFields(MerchantOrderListVo vo, RentOrderContext context) {
        if (context == null) {
            return;
        }
        RentOrder rentOrder = context.rentOrder;
        if (rentOrder != null) {
            vo.setRentType(rentOrder.getRentType());
            vo.setStartTime(rentOrder.getStartTime());
            vo.setEndTime(rentOrder.getEndTime());
        }
        List<MerchantOrderGoodsItemVo> items = toGoodsItemVos(context.items);
        vo.setItems(items);
        if (!items.isEmpty() && StringUtils.isNotBlank(items.get(0).getCoverImage())) {
            vo.setCoverImage(items.get(0).getCoverImage());
        }
    }

    private void fillRentGoodsFields(MerchantOrderDetailVo vo, RentOrderContext context) {
        if (context == null) {
            return;
        }
        RentOrder rentOrder = context.rentOrder;
        if (rentOrder != null) {
            vo.setRentType(rentOrder.getRentType());
            vo.setStartTime(rentOrder.getStartTime());
            vo.setEndTime(rentOrder.getEndTime());
            vo.setDays(rentOrder.getDays());
        }
        List<MerchantOrderGoodsItemVo> items = toGoodsItemVos(context.items);
        vo.setItems(items);
        if (!items.isEmpty() && StringUtils.isNotBlank(items.get(0).getCoverImage())) {
            vo.setCoverImage(items.get(0).getCoverImage());
        }
    }

    private List<MerchantOrderGoodsItemVo> toGoodsItemVos(List<RentOrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream().map(item -> {
            MerchantOrderGoodsItemVo vo = new MerchantOrderGoodsItemVo();
            vo.setGoodsId(item.getGoodsId());
            vo.setGoodsName(item.getGoodsName());
            vo.setCoverImage(item.getCoverImage());
            vo.setSpecInfo(item.getSpecInfo());
            vo.setPackageName(item.getPackageName());
            vo.setPackageDesc(item.getPackageDesc());
            vo.setQty(item.getQty());
            vo.setRentPrice(item.getRentPrice());
            return vo;
        }).collect(Collectors.toList());
    }

    private static final class RentOrderContext {
        private final RentOrder rentOrder;
        private final List<RentOrderItem> items;

        private RentOrderContext(RentOrder rentOrder, List<RentOrderItem> items) {
            this.rentOrder = rentOrder;
            this.items = items == null ? Collections.emptyList() : items;
        }
    }

    private String resolveNextAction(String status) {
        if (MerchantOrderStatus.PENDING_PREPARE.getCode().equals(status)) {
            return "prepare";
        }
        if (MerchantOrderStatus.PREPARED.getCode().equals(status)) {
            return "pickup";
        }
        if (MerchantOrderStatus.RETURNING.getCode().equals(status)
                || MerchantOrderStatus.INSPECTING.getCode().equals(status)) {
            return "inspection";
        }
        return "none";
    }

    private List<MerchantOrderTimelineVo> buildTimeline(MerchantOrder order, RentOrder rentOrder) {
        List<MerchantOrderTimelineVo> list = new ArrayList<>();
        addTimeline(list, "用户支付成功", order.getCreateTime());
        if (order.getPrepareTime() != null) {
            addTimeline(list, "商家确认备货", order.getPrepareTime());
        }
        if (order.getPickupTime() != null) {
            addTimeline(list, "商户核销取货", order.getPickupTime());
        }
        if (rentOrder != null && rentOrder.getReturnTime() != null) {
            addTimeline(list, "用户申请归还", rentOrder.getReturnTime());
        }
        if (order.getCompleteTime() != null) {
            addTimeline(list, "验收完成", order.getCompleteTime());
        }
        return list;
    }

    private void addTimeline(List<MerchantOrderTimelineVo> list, String title, LocalDateTime time) {
        MerchantOrderTimelineVo node = new MerchantOrderTimelineVo();
        node.setTitle(title);
        node.setTime(time == null ? "—" : time.format(DT_FMT));
        list.add(node);
    }

    private String resolvePickupLabel(String pickupType) {
        if (PickupType.DELIVERY.getCode().equals(pickupType)) {
            return "同城配送";
        }
        if (PickupType.PICKUP_POINT.getCode().equals(pickupType)) {
            return "驿站自提";
        }
        return "门店自提";
    }
}
