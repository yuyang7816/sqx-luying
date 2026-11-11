package com.sqx.app.merchant.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardRevenueVo;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardStatsVo;
import com.sqx.app.merchant.dashboard.domain.vo.MerchantDashboardTodayOrderVo;
import com.sqx.app.merchant.dashboard.service.MerchantDashboardService;
import com.sqx.app.merchant.enums.MerchantOrderStatus;
import com.sqx.app.merchant.order.domain.entity.MerchantOrder;
import com.sqx.app.merchant.order.mapper.MerchantOrderMapper;
import com.sqx.app.merchant.store.domain.entity.MerchantStore;
import com.sqx.app.merchant.store.mapper.MerchantStoreMapper;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MerchantDashboardServiceImpl implements MerchantDashboardService {

    private static final List<String> REFUND_STATUSES = Arrays.asList(
            OrderStatus.CANCELLED.getCode(),
            OrderStatus.REFUNDING.getCode());

    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private MerchantOrderMapper merchantOrderMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private MerchantStoreMapper merchantStoreMapper;

    @Override
    public MerchantDashboardStatsVo stats(Long userId) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        Long merchantId = ctx.getMerchantId();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        MerchantDashboardStatsVo vo = new MerchantDashboardStatsVo();
        fillStoreSummary(vo, merchantId, ctx.getStoreId());

        int goodsTotal = Math.toIntExact(rentGoodsMapper.selectCount(new LambdaQueryWrapper<RentGoods>()
                .eq(RentGoods::getMerchantId, merchantId)));
        int goodsOnShelf = Math.toIntExact(rentGoodsMapper.selectCount(new LambdaQueryWrapper<RentGoods>()
                .eq(RentGoods::getMerchantId, merchantId)
                .eq(RentGoods::getStatus, 1)));
        vo.setGoodsTotalCount(goodsTotal);
        vo.setGoodsOnShelfCount(goodsOnShelf);
        vo.setGoodsCount(goodsOnShelf);

        int orderTotal = Math.toIntExact(merchantOrderMapper.selectCount(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId)));
        vo.setOrderTotalCount(orderTotal);

        int pendingPrepare = countOrders(merchantId, MerchantOrderStatus.PENDING_PREPARE.getCode());
        int prepared = countOrders(merchantId, MerchantOrderStatus.PREPARED.getCode());
        vo.setPendingCount(pendingPrepare + prepared);
        vo.setPendingVerifyCount(prepared);

        vo.setReviewCount(countOrders(merchantId, MerchantOrderStatus.RETURNING.getCode())
                + countOrders(merchantId, MerchantOrderStatus.INSPECTING.getCode()));
        vo.setRentingCount(Math.toIntExact(rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getMerchantId, merchantId)
                .eq(RentOrder::getStatus, OrderStatus.RENTING.getCode()))));
        vo.setRefundedCount(Math.toIntExact(rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getMerchantId, merchantId)
                .in(RentOrder::getStatus, REFUND_STATUSES))));

        List<MerchantOrder> paidOrders = merchantOrderMapper.selectList(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId));
        vo.setTodayAmount(sumPayAmount(paidOrders, todayStart, todayEnd));
        vo.setMonthAmount(sumPayAmount(paidOrders, monthStart, todayEnd));
        vo.setTotalAmount(sumPayAmount(paidOrders, null, null));
        vo.setWeekAmount(sumPayAmount(paidOrders, today.minusDays(6).atStartOfDay(), todayEnd));
        vo.setTodayPickupOrders(listTodayPickupOrders(merchantId, todayStart, todayEnd));
        return vo;
    }

    @Override
    public MerchantDashboardRevenueVo revenue(Long userId, Integer year, Integer month) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        Long merchantId = ctx.getMerchantId();

        LocalDate today = LocalDate.now();
        int queryYear = year == null ? today.getYear() : year;
        int queryMonth = month == null ? 0 : month;

        LocalDateTime rangeStart;
        LocalDateTime rangeEnd;
        if (queryMonth <= 0) {
            rangeStart = LocalDate.of(queryYear, 1, 1).atStartOfDay();
            rangeEnd = LocalDate.of(queryYear, 12, 31).atTime(LocalTime.MAX);
        } else {
            YearMonth ym = YearMonth.of(queryYear, queryMonth);
            rangeStart = ym.atDay(1).atStartOfDay();
            rangeEnd = ym.atEndOfMonth().atTime(LocalTime.MAX);
        }

        List<MerchantOrder> orders = merchantOrderMapper.selectList(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId)
                .ge(MerchantOrder::getCreateTime, rangeStart)
                .le(MerchantOrder::getCreateTime, rangeEnd));

        MerchantDashboardRevenueVo vo = new MerchantDashboardRevenueVo();
        vo.setYear(queryYear);
        vo.setMonth(queryMonth);
        vo.setAmount(sumPayAmount(orders, rangeStart, rangeEnd));
        vo.setOrderCount(orders.size());

        List<MerchantOrder> allOrders = merchantOrderMapper.selectList(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId));
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        vo.setTodayAmount(sumPayAmount(allOrders, todayStart, todayEnd));
        vo.setMonthAmount(sumPayAmount(allOrders, monthStart, todayEnd));
        vo.setTotalAmount(sumPayAmount(allOrders, null, null));
        return vo;
    }

    private void fillStoreSummary(MerchantDashboardStatsVo vo, Long merchantId, Long storeId) {
        MerchantStore store = null;
        if (storeId != null) {
            store = merchantStoreMapper.selectById(storeId);
        }
        if (store == null && merchantId != null) {
            store = merchantStoreMapper.selectOne(new LambdaQueryWrapper<MerchantStore>()
                    .eq(MerchantStore::getMerchantId, merchantId)
                    .orderByAsc(MerchantStore::getId)
                    .last("LIMIT 1"));
        }
        if (store == null) {
            vo.setStoreName("商家工作台");
            vo.setBusinessStatus(1);
            return;
        }
        vo.setStoreName(store.getName());
        vo.setBusinessStatus(store.getStatus() == null ? 1 : store.getStatus());
    }

    private List<MerchantDashboardTodayOrderVo> listTodayPickupOrders(Long merchantId,
                                                                      LocalDateTime todayStart,
                                                                      LocalDateTime todayEnd) {
        List<MerchantOrder> candidates = merchantOrderMapper.selectList(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId)
                .in(MerchantOrder::getStatus,
                        MerchantOrderStatus.PENDING_PREPARE.getCode(),
                        MerchantOrderStatus.PREPARED.getCode())
                .orderByDesc(MerchantOrder::getCreateTime)
                .last("LIMIT 50"));
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> rentOrderIds = candidates.stream()
                .map(MerchantOrder::getRentOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (rentOrderIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, RentOrder> rentOrderMap = rentOrderMapper.selectBatchIds(rentOrderIds).stream()
                .collect(Collectors.toMap(RentOrder::getId, o -> o, (a, b) -> a));

        List<MerchantDashboardTodayOrderVo> result = new ArrayList<>();
        for (MerchantOrder order : candidates) {
            RentOrder rentOrder = rentOrderMap.get(order.getRentOrderId());
            if (rentOrder == null || rentOrder.getStartTime() == null) {
                continue;
            }
            LocalDateTime startTime = rentOrder.getStartTime();
            if (startTime.isBefore(todayStart) || startTime.isAfter(todayEnd)) {
                continue;
            }
            MerchantOrderStatus status = MerchantOrderStatus.fromCode(order.getStatus());
            MerchantDashboardTodayOrderVo item = new MerchantDashboardTodayOrderVo();
            item.setId(order.getId());
            item.setMerchantOrderNo(order.getMerchantOrderNo());
            item.setGoodsName(order.getGoodsName());
            item.setContactName(order.getContactName());
            item.setContactPhone(order.getContactPhone());
            item.setPickupStartTime(startTime);
            item.setStatusLabel(status.getDesc());
            item.setNextAction(resolveNextAction(order.getStatus()));
            item.setPayAmount(order.getPayAmount());
            result.add(item);
        }
        result.sort(Comparator.comparing(
                MerchantDashboardTodayOrderVo::getPickupStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ).thenComparing(MerchantDashboardTodayOrderVo::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private static String resolveNextAction(String status) {
        if (MerchantOrderStatus.PENDING_PREPARE.getCode().equals(status)) {
            return "prepare";
        }
        if (MerchantOrderStatus.PREPARED.getCode().equals(status)) {
            return "pickup";
        }
        return "none";
    }

    private int countOrders(Long merchantId, String status) {
        return Math.toIntExact(merchantOrderMapper.selectCount(new LambdaQueryWrapper<MerchantOrder>()
                .eq(MerchantOrder::getMerchantId, merchantId)
                .eq(MerchantOrder::getStatus, status)));
    }

    private static BigDecimal sumPayAmount(List<MerchantOrder> orders, LocalDateTime start, LocalDateTime end) {
        return orders.stream()
                .filter(o -> {
                    if (o.getCreateTime() == null) {
                        return false;
                    }
                    if (start != null && o.getCreateTime().isBefore(start)) {
                        return false;
                    }
                    if (end != null && o.getCreateTime().isAfter(end)) {
                        return false;
                    }
                    return true;
                })
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
