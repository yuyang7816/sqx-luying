package com.sqx.app.rental.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.enums.UserType;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.domain.entity.MerchantApply;
import com.sqx.app.merchant.apply.mapper.MerchantApplyMapper;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.enums.MerchantStatus;
import com.sqx.app.referral.commission.domain.entity.ReferralCommission;
import com.sqx.app.referral.commission.db.service.IReferralCommissionService;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawRecord;
import com.sqx.app.referral.withdraw.db.mapper.WithdrawRecordMapper;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import com.sqx.app.rental.dashboard.domain.vo.AdminRentDashboardStatsVo;
import com.sqx.app.rental.dashboard.domain.vo.AdminRentDashboardStatusItemVo;
import com.sqx.app.rental.dashboard.domain.vo.AdminRentDashboardTrendItemVo;
import com.sqx.app.rental.dashboard.service.AdminRentDashboardService;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentInspection;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.mapper.RentInspectionMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.user.domain.entity.UserFeedback;
import com.sqx.app.rental.user.enums.FeedbackStatus;
import com.sqx.app.rental.user.mapper.UserFeedbackMapper;
import com.sqx.app.rental.user.service.UserDepositService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AdminRentDashboardServiceImpl implements AdminRentDashboardService {

    private static final List<String> PAID_STATUSES = Arrays.asList(
            OrderStatus.WAITING_PICKUP.getCode(),
            OrderStatus.DELIVERING.getCode(),
            OrderStatus.RENTING.getCode(),
            OrderStatus.RETURNING.getCode(),
            OrderStatus.INSPECTING.getCode(),
            OrderStatus.COMPLETED.getCode(),
            OrderStatus.REFUNDING.getCode()
    );

    @Resource
    private RentCategoryMapper rentCategoryMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private UserFeedbackMapper userFeedbackMapper;
    @Resource
    private RentInspectionMapper rentInspectionMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantApplyMapper merchantApplyMapper;
    @Resource
    private WithdrawRecordMapper withdrawRecordMapper;
    @Resource
    private IReferralCommissionService iReferralCommissionService;
    @Resource
    private UserDepositService userDepositService;

    @Override
    public AdminRentDashboardStatsVo stats() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = LocalDate.now().atTime(LocalTime.MAX);

        AdminRentDashboardStatsVo vo = new AdminRentDashboardStatsVo();
        vo.setRentalCategoryTotal(Math.toIntExact(rentCategoryMapper.selectCount(new LambdaQueryWrapper<RentCategory>())));
        vo.setRentalGoodsTotal(Math.toIntExact(rentGoodsMapper.selectCount(new LambdaQueryWrapper<RentGoods>())));
        vo.setRentingOrderCount(Math.toIntExact(rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getStatus, OrderStatus.RENTING.getCode()))));

        long returning = rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getStatus, OrderStatus.RETURNING.getCode()));
        long inspecting = rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                .eq(RentOrder::getStatus, OrderStatus.INSPECTING.getCode()));
        long pendingInspect = rentInspectionMapper.selectCount(new LambdaQueryWrapper<RentInspection>()
                .eq(RentInspection::getAuditStatus, "pending"));
        vo.setPendingReturningCount(Math.toIntExact(returning));
        vo.setPendingInspectingCount(Math.toIntExact(inspecting));
        vo.setPendingInspectionCount(Math.toIntExact(pendingInspect));
        vo.setPendingCount(Math.toIntExact(returning + inspecting + pendingInspect));
        vo.setPendingFeedbackCount(Math.toIntExact(userFeedbackMapper.selectCount(new LambdaQueryWrapper<UserFeedback>()
                .eq(UserFeedback::getStatus, FeedbackStatus.PENDING.getCode()))));
        vo.setPendingMerchantApplyCount(Math.toIntExact(merchantApplyMapper.selectCount(new LambdaQueryWrapper<MerchantApply>()
                .in(MerchantApply::getStatus, "pending", "reviewing"))));
        vo.setPendingWithdrawCount(Math.toIntExact(withdrawRecordMapper.selectCount(new LambdaQueryWrapper<WithdrawRecord>()
                .eq(WithdrawRecord::getStatus, 1))));
        vo.setPendingUserDepositRefundCount(userDepositService.countPendingRefund());

        List<RentOrder> todayOrders = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>()
                .ge(RentOrder::getCreateTime, dayStart)
                .le(RentOrder::getCreateTime, dayEnd));
        vo.setTodayOrderCount(todayOrders.size());
        BigDecimal todayPay = todayOrders.stream()
                .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTodayPayAmount(todayPay);

        vo.setTotalAppUserCount(Math.toIntExact(userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserType, UserType.USER.getCode()))));
        vo.setTotalMerchantCount(Math.toIntExact(merchantMapper.selectCount(new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, MerchantStatus.ACTIVE.getCode()))));
        vo.setTodayNewUserCount(Math.toIntExact(userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserType, UserType.USER.getCode())
                .ge(UserEntity::getCreateTime, dayStart)
                .le(UserEntity::getCreateTime, dayEnd))));
        vo.setTodayInviteRegisterCount(Math.toIntExact(userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserType, UserType.USER.getCode())
                .isNotNull(UserEntity::getSuperiorInvitationCode)
                .ne(UserEntity::getSuperiorInvitationCode, "")
                .ge(UserEntity::getCreateTime, dayStart)
                .le(UserEntity::getCreateTime, dayEnd))));

        vo.setTotalReferralCommission(sumOrderCommission());

        List<AdminRentDashboardTrendItemVo> trend = buildOrderTrend(7);
        vo.setOrderTrend(trend);
        vo.setWeekOrderCount(trend.stream().mapToInt(item -> item.getOrderCount() == null ? 0 : item.getOrderCount()).sum());
        vo.setWeekPayAmount(trend.stream()
                .map(item -> item.getPayAmount() == null ? BigDecimal.ZERO : item.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        vo.setOrderStatusDistribution(buildOrderStatusDistribution());
        return vo;
    }

    private List<AdminRentDashboardTrendItemVo> buildOrderTrend(int days) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        List<AdminRentDashboardTrendItemVo> trend = new ArrayList<>();
        for (int offset = days - 1; offset >= 0; offset--) {
            LocalDate date = LocalDate.now().minusDays(offset);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            List<RentOrder> dayOrders = rentOrderMapper.selectList(new LambdaQueryWrapper<RentOrder>()
                    .ge(RentOrder::getCreateTime, start)
                    .le(RentOrder::getCreateTime, end));
            AdminRentDashboardTrendItemVo item = new AdminRentDashboardTrendItemVo();
            item.setDate(date.format(formatter));
            item.setOrderCount(dayOrders.size());
            BigDecimal payAmount = dayOrders.stream()
                    .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                    .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            item.setPayAmount(payAmount);
            trend.add(item);
        }
        return trend;
    }

    private List<AdminRentDashboardStatusItemVo> buildOrderStatusDistribution() {
        List<AdminRentDashboardStatusItemVo> distribution = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = rentOrderMapper.selectCount(new LambdaQueryWrapper<RentOrder>()
                    .eq(RentOrder::getStatus, status.getCode()));
            if (count <= 0) {
                continue;
            }
            AdminRentDashboardStatusItemVo item = new AdminRentDashboardStatusItemVo();
            item.setStatus(status.getCode());
            item.setLabel(status.getDesc());
            item.setCount(Math.toIntExact(count));
            distribution.add(item);
        }
        return distribution;
    }

    private BigDecimal sumOrderCommission() {
        try {
            List<ReferralCommission> commissionRows = iReferralCommissionService.list(new LambdaQueryWrapper<ReferralCommission>()
                    .eq(ReferralCommission::getType, 1)
                    .select(ReferralCommission::getAmount, ReferralCommission::getDirection));
            return commissionRows.stream()
                    .filter(row -> row.getDirection() == null || row.getDirection() == 1)
                    .map(ReferralCommission::getAmount)
                    .filter(amount -> amount != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}
