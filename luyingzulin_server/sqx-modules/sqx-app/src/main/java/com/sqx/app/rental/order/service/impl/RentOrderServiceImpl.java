package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.cart.service.RentCartService;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PayBizType;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.enums.RentType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import com.sqx.app.rental.goods.enums.RentGoodsSpecType;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsPackageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsSpecMapper;
import com.sqx.app.rental.order.domain.bo.AppRentOrderPageBo;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateBo;
import com.sqx.app.rental.order.domain.bo.RentOrderPageBo;
import com.sqx.app.rental.order.domain.bo.RentOrderCreateItemBo;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.domain.entity.RentOrderLog;
import com.sqx.app.rental.order.domain.entity.RentOrderPayment;
import com.sqx.app.rental.order.domain.entity.RentInspection;
import com.sqx.app.rental.order.domain.entity.RentRenewRecord;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.domain.vo.AppRentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentInspectionDetailVo;
import com.sqx.app.rental.order.domain.vo.RentRenewRecordVo;
import com.sqx.app.rental.order.domain.vo.RentReturnDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderDetailVo;
import com.sqx.app.rental.order.domain.vo.RentOrderItemVo;
import com.sqx.app.rental.order.domain.vo.RentOrderListVo;
import com.sqx.app.rental.order.domain.vo.RentOrderLogVo;
import com.sqx.app.rental.order.domain.vo.RentOrderPayStatusVo;
import com.sqx.app.rental.order.domain.entity.RentPickupCode;
import com.sqx.app.rental.order.domain.vo.RentOrderCreateVo;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentOrderPaymentMapper;
import com.sqx.app.rental.order.mapper.RentInspectionMapper;
import com.sqx.app.rental.order.mapper.RentRenewRecordMapper;
import com.sqx.app.rental.order.mapper.RentPickupCodeMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.app.rental.order.service.RentOrderService;
import com.sqx.app.rental.order.domain.vo.RentOrderStatusCountVo;
import com.sqx.app.rental.order.support.RentOrderPriceCalculator;
import com.sqx.app.rental.order.support.RentalDeliveryFeeSupport;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.app.rental.user.service.UserCouponService;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.app.wallet.wallet.support.RentalWalletFlowService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.core.utils.SearchKeywordUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租赁订单服务实现
 */
@Slf4j
@Service
public class RentOrderServiceImpl implements RentOrderService {

    private static final Set<String> WAITING_PICKUP_STATUSES = new HashSet<>(Arrays.asList(
            OrderStatus.WAITING_PICKUP.getCode(),
            OrderStatus.DELIVERING.getCode()
    ));

    private static final Set<String> RETURNING_STATUSES = new HashSet<>(Arrays.asList(
            OrderStatus.RETURNING.getCode(),
            OrderStatus.INSPECTING.getCode()
    ));

    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private RentOrderPaymentMapper rentOrderPaymentMapper;
    @Resource
    private RentPickupCodeMapper rentPickupCodeMapper;
    @Resource
    private RentRenewRecordMapper rentRenewRecordMapper;
    @Resource
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Resource
    private RentInspectionMapper rentInspectionMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentGoodsSpecMapper rentGoodsSpecMapper;
    @Resource
    private RentGoodsPackageMapper rentGoodsPackageMapper;
    @Resource
    private RentStockService rentStockService;
    @Resource
    private RentCartService rentCartService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserCouponService userCouponService;
    @Resource
    private RentalDeliveryFeeSupport rentalDeliveryFeeSupport;
    @Resource
    private UserDepositService userDepositService;
    @Resource
    private RentalWalletFlowService rentalWalletFlowService;
    @Resource
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Resource
    private MerchantStoreOpenSupport merchantStoreOpenSupport;
    @Resource
    private com.sqx.app.rental.order.support.RentOrderNotificationSupport rentOrderNotificationSupport;
    @Resource
    private com.sqx.app.rental.order.support.RentOrderRefundSupport rentOrderRefundSupport;

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public RentOrderStatusCountVo statusCounts(Long userId) {
        RentOrderStatusCountVo vo = new RentOrderStatusCountVo();
        vo.setPendingPayment(0);
        vo.setWaitingPickup(0);
        vo.setRenting(0);
        vo.setReturning(0);
        vo.setAfterSale(0);

        List<RentOrder> orders = rentOrderMapper.selectList(new QueryWrapper<RentOrder>()
                .eq("user_id", userId)
                .select("status"));
        if (CollectionUtils.isEmpty(orders)) {
            return vo;
        }

        for (RentOrder order : orders) {
            String status = order.getStatus();
            if (OrderStatus.PENDING_PAYMENT.getCode().equals(status)) {
                vo.setPendingPayment(vo.getPendingPayment() + 1);
            } else if (WAITING_PICKUP_STATUSES.contains(status)) {
                vo.setWaitingPickup(vo.getWaitingPickup() + 1);
            } else if (OrderStatus.RENTING.getCode().equals(status)) {
                vo.setRenting(vo.getRenting() + 1);
            } else if (RETURNING_STATUSES.contains(status)) {
                vo.setReturning(vo.getReturning() + 1);
            } else if (OrderStatus.REFUNDING.getCode().equals(status)) {
                vo.setAfterSale(vo.getAfterSale() + 1);
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentOrderCreateVo createOrder(Long userId, RentOrderCreateBo createBo) {
        PickupType.fromCode(createBo.getPickupType());
        if (CollectionUtils.isEmpty(createBo.getItems())) {
            throw new BizException("商品明细不能为空");
        }

        BigDecimal rentAmount = BigDecimal.ZERO;
        BigDecimal depositAmount = BigDecimal.ZERO;
        LocalDateTime orderStart = null;
        LocalDateTime orderEnd = null;
        String orderRentType = createBo.getItems().get(0).getRentType();
        Long merchantId = null;

        for (RentOrderCreateItemBo itemBo : createBo.getItems()) {
            RentType.fromCode(itemBo.getRentType());
            if (!itemBo.getEndTime().isAfter(itemBo.getStartTime())) {
                throw new BizException("租期结束时间必须晚于开始时间");
            }
            RentGoods goods = requireOnShelfGoods(itemBo.getGoodsId());
            if (merchantId == null) {
                merchantId = goods.getMerchantId();
            }
            RentGoodsPackage goodsPackage = null;
            if (itemBo.getPackageId() != null) {
                goodsPackage = rentGoodsPackageMapper.selectById(itemBo.getPackageId());
                if (goodsPackage == null || !goods.getId().equals(goodsPackage.getGoodsId())) {
                    throw new BizException("配件套餐不存在");
                }
            }
            RentGoodsSpec goodsSpec = resolveOrderSpec(goods.getId(), itemBo.getSpecId());
            int qty = itemBo.getQuantity() == null ? 1 : itemBo.getQuantity();
            rentAmount = rentAmount.add(RentOrderPriceCalculator.calcItemRentAmount(
                    goods, itemBo.getRentType(), itemBo.getStartTime(), itemBo.getEndTime(),
                    itemBo.getDays(), qty, goodsPackage, goodsSpec));
            depositAmount = depositAmount.add(RentOrderPriceCalculator.calcItemDeposit(goods, qty));

            if (orderStart == null || itemBo.getStartTime().isBefore(orderStart)) {
                orderStart = itemBo.getStartTime();
            }
            if (orderEnd == null || itemBo.getEndTime().isAfter(orderEnd)) {
                orderEnd = itemBo.getEndTime();
            }
        }

        BigDecimal deliveryAmount = rentalDeliveryFeeSupport.calc(
                createBo.getPickupType(), createBo.getPickupStoreId(), rentAmount);
        BigDecimal couponAmount = userCouponService.previewDiscount(userId, createBo.getCouponId(),
                rentAmount, deliveryAmount, depositAmount);
        userDepositService.assertSufficientForOrder(userId, depositAmount);
        BigDecimal payAmount = RentOrderPriceCalculator.calcPayAmount(
                rentAmount, depositAmount, deliveryAmount, couponAmount);

        String orderNo = GenUtil.genOrderNo();
        RentOrder order = new RentOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setRentType(orderRentType);
        order.setStartTime(orderStart);
        order.setEndTime(orderEnd);
        order.setDays(RentOrderPriceCalculator.resolveDays(orderStart, orderEnd, null));
        order.setPickupType(createBo.getPickupType());
        order.setPickupStoreId(createBo.getPickupStoreId());
        order.setAddressId(createBo.getAddressId());
        order.setContactName(createBo.getContactName());
        order.setContactPhone(createBo.getContactPhone());
        order.setRemark(createBo.getRemark());
        order.setRentAmount(rentAmount);
        order.setDepositAmount(depositAmount);
        order.setDeliveryAmount(deliveryAmount);
        order.setCouponAmount(couponAmount);
        order.setCouponId(createBo.getCouponId());
        order.setPayAmount(payAmount);
        rentOrderMapper.insert(order);

        for (RentOrderCreateItemBo itemBo : createBo.getItems()) {
            RentGoods goods = requireOnShelfGoods(itemBo.getGoodsId());
            Long specId = itemBo.getSpecId() == null ? 0L : itemBo.getSpecId();
            RentGoodsSpec goodsSpec = resolveOrderSpec(goods.getId(), specId);
            int qty = itemBo.getQuantity() == null ? 1 : itemBo.getQuantity();
            RentGoodsPackage goodsPackage = null;
            if (itemBo.getPackageId() != null) {
                goodsPackage = rentGoodsPackageMapper.selectById(itemBo.getPackageId());
                if (goodsPackage == null || !goods.getId().equals(goodsPackage.getGoodsId())) {
                    throw new BizException("配件套餐不存在");
                }
            }

            RentOrderItem item = new RentOrderItem();
            item.setOrderId(order.getId());
            item.setOrderNo(orderNo);
            item.setGoodsId(itemBo.getGoodsId());
            item.setGoodsName(goods.getName());
            item.setCoverImage(goods.getCoverImage());
            item.setSpecInfo(buildSpecInfo(goodsSpec));
            if (goodsPackage != null) {
                item.setPackageId(goodsPackage.getId());
                item.setPackageName(goodsPackage.getPackageName());
                item.setPackageDesc(goodsPackage.getPackageDesc());
            }
            item.setRentType(itemBo.getRentType());
            item.setStartTime(itemBo.getStartTime());
            item.setEndTime(itemBo.getEndTime());
            item.setDays(RentOrderPriceCalculator.resolveDays(itemBo.getStartTime(), itemBo.getEndTime(), itemBo.getDays()));
            item.setQty(qty);
            item.setRentPrice(RentOrderPriceCalculator.unitRentPrice(goods, itemBo.getRentType()));
            item.setDepositAmount(RentOrderPriceCalculator.calcItemDeposit(goods, 1));
            BigDecimal itemRent = RentOrderPriceCalculator.calcItemRentAmount(
                    goods, itemBo.getRentType(), itemBo.getStartTime(), itemBo.getEndTime(),
                    itemBo.getDays(), qty, goodsPackage, goodsSpec);
            item.setItemAmount(itemRent.add(RentOrderPriceCalculator.calcItemDeposit(goods, qty)));
            rentOrderItemMapper.insert(item);

            rentStockService.prelock(order.getId(), orderNo, itemBo.getGoodsId(), specId,
                    itemBo.getStartTime(), itemBo.getEndTime(), qty);
        }

        writeOrderLog(order.getId(), orderNo, null, OrderStatus.PENDING_PAYMENT.getCode(),
                "user", userId, "创建订单");

        if (CollectionUtils.isNotEmpty(createBo.getCartIds())) {
            rentCartService.removeByIds(userId, createBo.getCartIds());
        }
        if (createBo.getCouponId() != null) {
            userCouponService.consumeForOrder(userId, createBo.getCouponId(), orderNo);
        }

        RentOrderCreateVo vo = new RentOrderCreateVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(orderNo);
        vo.setPayAmount(payAmount);
        vo.setDepositAmount(depositAmount);
        vo.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long userId, Long orderId) {
        RentOrder order = requireOwnedOrder(userId, orderId);
        RentOrderStateMachine.assertCanCancel(order.getStatus());
        String fromStatus = order.getStatus();
        boolean needsRefund = RentOrderStateMachine.requiresRefundOnCancel(fromStatus);

        int updated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, orderId)
                .eq(RentOrder::getStatus, fromStatus)
                .set(RentOrder::getStatus, OrderStatus.CANCELLED.getCode()));
        if (updated == 0) {
            throw new BizException("订单状态已变更，请刷新后重试");
        }

        rentStockService.releaseOccupy(orderId);
        if (needsRefund) {
            rentOrderRefundSupport.refundPaidOrder(order, "用户取消订单（未取货）");
            userDepositService.unfreezeForCancelledOrder(order.getUserId(), order.getOrderNo(), order.getDepositAmount());
        }
        writeOrderLog(orderId, order.getOrderNo(), fromStatus,
                OrderStatus.CANCELLED.getCode(), "user", userId,
                needsRefund ? "用户取消订单并退款" : "用户取消订单");
        rentOrderNotificationSupport.onCancel(order,
                needsRefund ? "您已取消订单，款项将原路退回。" : "您已取消该订单。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(String orderNo, String payChannel, String payNo, BigDecimal paidAmount) {
        if (StringUtils.isBlank(orderNo)) {
            throw new BizException("订单号不能为空");
        }
        RentOrder order = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }

        if (RentOrderStateMachine.isPaidStatus(order.getStatus())) {
            return;
        }
        RentOrderStateMachine.assertCanPay(order.getStatus());

        if (paidAmount != null && order.getPayAmount() != null
                && paidAmount.compareTo(order.getPayAmount()) != 0) {
            throw new BizException("支付金额与订单不符");
        }

        String targetStatus = RentOrderStateMachine.resolvePaidStatus(order.getPickupType());
        String pickupCode = RentOrderPriceCalculator.generatePickupCode();
        LocalDateTime now = LocalDateTime.now();

        int updated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, order.getId())
                .eq(RentOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .set(RentOrder::getStatus, targetStatus)
                .set(RentOrder::getPickupCode, pickupCode)
                .set(RentOrder::getPayTime, now));
        if (updated == 0) {
            RentOrder latest = rentOrderMapper.selectById(order.getId());
            if (latest != null && RentOrderStateMachine.isPaidStatus(latest.getStatus())) {
                return;
            }
            throw new BizException("订单状态已变更，支付处理失败");
        }

        rentStockService.confirmOccupy(order.getId());

        RentPickupCode codeEntity = new RentPickupCode();
        codeEntity.setOrderId(order.getId());
        codeEntity.setOrderNo(orderNo);
        codeEntity.setPickupCode(pickupCode);
        codeEntity.setExpireTime(order.getEndTime());
        codeEntity.setUsed(0);
        rentPickupCodeMapper.insert(codeEntity);

        RentOrderPayment payment = new RentOrderPayment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(orderNo);
        payment.setPayNo(payNo);
        payment.setPayBizType(PayBizType.RENT_ORDER.getCode());
        payment.setAmount(order.getPayAmount());
        payment.setPayChannel(payChannel);
        payment.setPayStatus("success");
        payment.setPayTime(now);
        rentOrderPaymentMapper.insert(payment);

        writeOrderLog(order.getId(), orderNo, OrderStatus.PENDING_PAYMENT.getCode(),
                targetStatus, "system", null, "支付成功");

        BigDecimal rentPayAmount = (order.getRentAmount() == null ? BigDecimal.ZERO : order.getRentAmount())
                .add(order.getDeliveryAmount() == null ? BigDecimal.ZERO : order.getDeliveryAmount())
                .subtract(order.getCouponAmount() == null ? BigDecimal.ZERO : order.getCouponAmount())
                .max(BigDecimal.ZERO);
        if (!"balance".equals(payChannel)) {
            rentalWalletFlowService.recordRentPay(order.getUserId(), order.getId(), orderNo, rentPayAmount);
        }
        userDepositService.freezeForOrder(order.getUserId(), orderNo, order.getDepositAmount());

        try {
            merchantOrderSyncSupport.syncOnPaySuccess(order);
        } catch (Exception e) {
            log.warn("同步商户订单失败 orderNo={}: {}", orderNo, e.getMessage());
        }
        rentOrderNotificationSupport.onPaySuccess(order);
    }

    @Override
    public BigDecimal prepareRentOrderPay(Long userId, String orderNo, BigDecimal requestAmount) {
        RentOrder order = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException("无权支付该订单");
        }
        RentOrderStateMachine.assertCanPay(order.getStatus());
        if (requestAmount != null && order.getPayAmount() != null
                && normalizeMoney(requestAmount).compareTo(normalizeMoney(order.getPayAmount())) != 0) {
            throw new BizException("支付金额与订单不符");
        }
        return order.getPayAmount();
    }

    private static BigDecimal normalizeMoney(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public RentOrderPayStatusVo payStatus(Long userId, String orderNo) {
        RentOrder order = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getOrderNo, orderNo)
                .eq(RentOrder::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        RentOrderPayStatusVo vo = new RentOrderPayStatusVo();
        vo.setOrderNo(orderNo);
        vo.setStatus(order.getStatus());
        vo.setPaid(RentOrderStateMachine.isPaidStatus(order.getStatus()));
        vo.setPickupCode(order.getPickupCode());
        return vo;
    }

    @Override
    public IPage<RentOrderListVo> adminPage(RentOrderPageBo pageBo) {
        Page<RentOrder> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<RentOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getStatus())) {
            wrapper.eq(RentOrder::getStatus, pageBo.getStatus());
        }
        if (pageBo.getMerchantId() != null) {
            wrapper.eq(RentOrder::getMerchantId, pageBo.getMerchantId());
        }
        if (StringUtils.isNotBlank(pageBo.getStartTime())) {
            wrapper.ge(RentOrder::getCreateTime, LocalDateTime.parse(pageBo.getStartTime(), DATE_TIME_FMT));
        }
        if (StringUtils.isNotBlank(pageBo.getEndTime())) {
            wrapper.le(RentOrder::getCreateTime, LocalDateTime.parse(pageBo.getEndTime(), DATE_TIME_FMT));
        }
        applyKeywordFilter(wrapper, pageBo.getKeyword());
        wrapper.orderByDesc(RentOrder::getCreateTime);

        IPage<RentOrder> orderPage = rentOrderMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(orderPage.getRecords())) {
            return PageUtil.copyPage(orderPage, Collections.emptyList());
        }

        List<Long> orderIds = orderPage.getRecords().stream().map(RentOrder::getId).collect(Collectors.toList());
        List<Long> userIds = orderPage.getRecords().stream().map(RentOrder::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UserEntity> userMap = loadUserMap(userIds);
        Map<Long, List<RentOrderItem>> itemMap = loadOrderItemMap(orderIds);

        List<RentOrderListVo> records = orderPage.getRecords().stream()
                .map(order -> toListVo(order, userMap.get(order.getUserId()), itemMap.get(order.getId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(orderPage, records);
    }

    @Override
    public RentOrderDetailVo adminDetail(Long orderId) {
        RentOrder order = rentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        UserEntity user = order.getUserId() == null ? null : userMapper.selectById(order.getUserId());
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, orderId)
                .orderByAsc(RentOrderItem::getId));
        List<RentOrderLog> logs = rentOrderLogMapper.selectList(new LambdaQueryWrapper<RentOrderLog>()
                .eq(RentOrderLog::getOrderId, orderId)
                .orderByAsc(RentOrderLog::getCreateTime));

        RentOrderDetailVo vo = new RentOrderDetailVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setUserId(order.getUserId());
        vo.setUserNickName(user == null ? null : user.getUserName());
        vo.setUserPhone(user == null ? null : user.getPhone());
        vo.setRentType(order.getRentType());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setDays(order.getDays());
        vo.setPickupType(order.getPickupType());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setPickupCode(order.getPickupCode());
        vo.setRentAmount(order.getRentAmount());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setDeliveryAmount(order.getDeliveryAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setPickupTime(order.getPickupTime());
        vo.setReturnTime(order.getReturnTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setItems(items.stream().map(this::toItemVo).collect(Collectors.toList()));
        vo.setLogs(logs.stream().map(this::toLogVo).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminCancelOrder(Long orderId) {
        RentOrder order = rentOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        RentOrderStateMachine.assertCanCancel(order.getStatus());

        rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, orderId)
                .eq(RentOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                .set(RentOrder::getStatus, OrderStatus.CANCELLED.getCode()));

        rentStockService.releaseOccupy(orderId);
        writeOrderLog(orderId, order.getOrderNo(), OrderStatus.PENDING_PAYMENT.getCode(),
                OrderStatus.CANCELLED.getCode(), "admin", null, "管理端代取消订单");
    }

    @Override
    public IPage<RentOrderListVo> appPage(Long userId, AppRentOrderPageBo pageBo) {
        Page<RentOrder> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<RentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RentOrder::getUserId, userId);
        applyAppStatusFilter(wrapper, pageBo.getStatus());
        wrapper.orderByDesc(RentOrder::getCreateTime);

        IPage<RentOrder> orderPage = rentOrderMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(orderPage.getRecords())) {
            return PageUtil.copyPage(orderPage, Collections.emptyList());
        }

        List<Long> orderIds = orderPage.getRecords().stream().map(RentOrder::getId).collect(Collectors.toList());
        Map<Long, List<RentOrderItem>> itemMap = loadOrderItemMap(orderIds);

        List<RentOrderListVo> records = orderPage.getRecords().stream()
                .map(order -> toListVo(order, null, itemMap.get(order.getId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(orderPage, records);
    }

    @Override
    public AppRentOrderDetailVo appDetail(Long userId, Long orderId) {
        RentOrder order = requireOwnedOrder(userId, orderId);
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, orderId)
                .orderByAsc(RentOrderItem::getId));
        List<RentOrderLog> logs = rentOrderLogMapper.selectList(new LambdaQueryWrapper<RentOrderLog>()
                .eq(RentOrderLog::getOrderId, orderId)
                .orderByAsc(RentOrderLog::getCreateTime));
        List<RentRenewRecord> renewRecords = rentRenewRecordMapper.selectList(new LambdaQueryWrapper<RentRenewRecord>()
                .eq(RentRenewRecord::getOrderId, orderId)
                .orderByAsc(RentRenewRecord::getCreateTime));
        RentReturnApply returnApply = rentReturnApplyMapper.selectOne(new LambdaQueryWrapper<RentReturnApply>()
                .eq(RentReturnApply::getOrderId, orderId)
                .orderByDesc(RentReturnApply::getId)
                .last("LIMIT 1"));
        RentInspection inspection = rentInspectionMapper.selectOne(new LambdaQueryWrapper<RentInspection>()
                .eq(RentInspection::getOrderId, orderId)
                .orderByDesc(RentInspection::getId)
                .last("LIMIT 1"));

        AppRentOrderDetailVo vo = new AppRentOrderDetailVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setRentType(order.getRentType());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setDays(order.getDays());
        vo.setPickupType(order.getPickupType());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setPickupCode(order.getPickupCode());
        vo.setRentAmount(order.getRentAmount());
        vo.setDepositAmount(order.getDepositAmount());
        vo.setDeliveryAmount(order.getDeliveryAmount());
        vo.setCouponAmount(order.getCouponAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setPickupTime(order.getPickupTime());
        vo.setReturnTime(order.getReturnTime());
        vo.setCompleteTime(order.getCompleteTime());
        vo.setItems(items.stream().map(this::toItemVo).collect(Collectors.toList()));
        vo.setLogs(logs.stream().map(this::toLogVo).collect(Collectors.toList()));
        vo.setRenewRecords(renewRecords.stream().map(this::toRenewVo).collect(Collectors.toList()));
        if (returnApply != null) {
            vo.setReturnApply(RentReturnServiceImpl.toDetailVo(order, returnApply));
        }
        if (inspection != null) {
            vo.setInspection(RentInspectionServiceImpl.toDetailVo(inspection, null));
        }
        return vo;
    }

    private void applyAppStatusFilter(LambdaQueryWrapper<RentOrder> wrapper, String status) {
        if (StringUtils.isBlank(status) || "all".equals(status)) {
            return;
        }
        if (OrderStatus.WAITING_PICKUP.getCode().equals(status)) {
            wrapper.in(RentOrder::getStatus, RentOrderStateMachine.waitingPickupTabStatuses());
            return;
        }
        if (OrderStatus.RETURNING.getCode().equals(status)) {
            wrapper.in(RentOrder::getStatus, RentOrderStateMachine.returningTabStatuses());
            return;
        }
        wrapper.eq(RentOrder::getStatus, status);
    }

    private RentRenewRecordVo toRenewVo(RentRenewRecord record) {
        RentRenewRecordVo vo = new RentRenewRecordVo();
        vo.setRenewNo(record.getRenewNo());
        vo.setExtraDays(record.getExtraDays());
        vo.setRenewAmount(record.getRenewAmount());
        vo.setOldEndTime(record.getOldEndTime());
        vo.setNewEndTime(record.getNewEndTime());
        vo.setPayStatus(record.getPayStatus());
        vo.setPayTime(record.getPayTime());
        return vo;
    }

    private void applyKeywordFilter(LambdaQueryWrapper<RentOrder> wrapper, String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return;
        }
        Long orderId = SearchKeywordUtil.parseLongId(keyword);
        if (orderId != null) {
            wrapper.eq(RentOrder::getId, orderId);
            return;
        }
        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                        .like(UserEntity::getPhone, keyword)
                        .select(UserEntity::getUserId))
                .stream()
                .map(UserEntity::getUserId)
                .collect(Collectors.toList());
        List<Long> orderIdsByGoods = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                        .like(RentOrderItem::getGoodsName, keyword)
                        .select(RentOrderItem::getOrderId))
                .stream()
                .map(RentOrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());

        wrapper.and(w -> {
            w.like(RentOrder::getContactPhone, keyword);
            if (CollectionUtils.isNotEmpty(userIds)) {
                w.or().in(RentOrder::getUserId, userIds);
            }
            if (CollectionUtils.isNotEmpty(orderIdsByGoods)) {
                w.or().in(RentOrder::getId, orderIdsByGoods);
            }
        });
    }

    private Map<Long, UserEntity> loadUserMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
    }

    private Map<Long, List<RentOrderItem>> loadOrderItemMap(List<Long> orderIds) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .in(RentOrderItem::getOrderId, orderIds)
                .orderByAsc(RentOrderItem::getId));
        Map<Long, List<RentOrderItem>> map = new HashMap<>();
        for (RentOrderItem item : items) {
            map.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    private RentOrderListVo toListVo(RentOrder order, UserEntity user, List<RentOrderItem> items) {
        RentOrderListVo vo = new RentOrderListVo();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setUserId(order.getUserId());
        vo.setUserNickName(user == null ? null : user.getUserName());
        vo.setUserPhone(user == null ? null : user.getPhone());
        vo.setGoodsSummary(buildGoodsSummary(items));
        vo.setCoverImage(resolveFirstCoverImage(items));
        vo.setRentType(order.getRentType());
        vo.setStartTime(order.getStartTime());
        vo.setEndTime(order.getEndTime());
        vo.setDays(order.getDays());
        vo.setPickupType(order.getPickupType());
        vo.setContactName(order.getContactName());
        vo.setContactPhone(order.getContactPhone());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items.stream().map(this::toItemVo).collect(Collectors.toList()));
        return vo;
    }

    private String buildGoodsSummary(List<RentOrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        String first = items.get(0).getGoodsName();
        if (items.size() == 1) {
            return first;
        }
        return first + " 等" + items.size() + "件";
    }

    private String resolveFirstCoverImage(List<RentOrderItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return null;
        }
        for (RentOrderItem item : items) {
            if (StringUtils.isNotBlank(item.getCoverImage())) {
                return item.getCoverImage();
            }
        }
        return null;
    }

    private RentOrderItemVo toItemVo(RentOrderItem item) {
        RentOrderItemVo vo = new RentOrderItemVo();
        vo.setId(item.getId());
        vo.setGoodsId(item.getGoodsId());
        vo.setGoodsName(item.getGoodsName());
        vo.setSpecInfo(item.getSpecInfo());
        vo.setPackageId(item.getPackageId());
        vo.setPackageName(item.getPackageName());
        vo.setPackageDesc(item.getPackageDesc());
        vo.setCoverImage(item.getCoverImage());
        vo.setRentType(item.getRentType());
        vo.setStartTime(item.getStartTime());
        vo.setEndTime(item.getEndTime());
        vo.setQty(item.getQty());
        vo.setRentPrice(item.getRentPrice());
        vo.setDepositAmount(item.getDepositAmount());
        vo.setItemAmount(item.getItemAmount());
        return vo;
    }

    private RentOrderLogVo toLogVo(RentOrderLog log) {
        RentOrderLogVo vo = new RentOrderLogVo();
        vo.setId(log.getId());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setOperatorType(log.getOperatorType());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    private RentOrder requireOwnedOrder(Long userId, Long orderId) {
        RentOrder order = rentOrderMapper.selectOne(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getId, orderId)
                .eq(RentOrder::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("订单不存在");
        }
        return order;
    }

    private RentGoods requireOnShelfGoods(Long goodsId) {
        RentGoods goods = rentGoodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品不存在或未上架");
        }
        merchantStoreOpenSupport.assertMerchantStoreOpen(goods.getMerchantId());
        return goods;
    }

    private RentGoodsSpec resolveOrderSpec(Long goodsId, Long specId) {
        long normalizedSpecId = specId == null ? 0L : specId;
        long skuCount = rentGoodsSpecMapper.selectCount(new LambdaQueryWrapper<RentGoodsSpec>()
                .eq(RentGoodsSpec::getGoodsId, goodsId)
                .eq(RentGoodsSpec::getSpecType, RentGoodsSpecType.SKU));
        if (skuCount <= 0) {
            skuCount = rentGoodsSpecMapper.selectCount(new LambdaQueryWrapper<RentGoodsSpec>()
                    .eq(RentGoodsSpec::getGoodsId, goodsId)
                    .apply("spec_value REGEXP '^[0-9]+(\\\\.[0-9]+)?$'"));
        }
        if (skuCount > 0 && normalizedSpecId <= 0) {
            throw new BizException("请选择商品规格");
        }
        if (normalizedSpecId <= 0) {
            return null;
        }
        RentGoodsSpec spec = rentGoodsSpecMapper.selectById(normalizedSpecId);
        if (spec == null || !goodsId.equals(spec.getGoodsId())) {
            throw new BizException("商品规格不存在");
        }
        int specType = spec.getSpecType() != null ? spec.getSpecType() : RentGoodsSpecType.SKU;
        if (specType != RentGoodsSpecType.SKU) {
            throw new BizException("请选择有效的商品规格");
        }
        return spec;
    }

    private String buildSpecInfo(RentGoodsSpec spec) {
        if (spec == null) {
            return null;
        }
        return spec.getSpecName();
    }

    private String buildSpecInfo(Long specId) {
        if (specId == null || specId <= 0) {
            return null;
        }
        RentGoodsSpec spec = rentGoodsSpecMapper.selectById(specId);
        return buildSpecInfo(spec);
    }

    private void writeOrderLog(Long orderId, String orderNo, String fromStatus, String toStatus,
                               String operatorType, Long operatorId, String remark) {
        RentOrderLog log = new RentOrderLog();
        log.setOrderId(orderId);
        log.setOrderNo(orderNo);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorType(operatorType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        rentOrderLogMapper.insert(log);
    }
}
