package com.sqx.app.vip.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.vip.order.domain.entity.VipOrder;
import com.sqx.app.vip.order.domain.vo.VipPayStatusVo;
import com.sqx.app.vip.order.domain.vo.VipPurchaseVo;
import com.sqx.app.vip.order.mapper.VipOrderMapper;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.app.vip.plan.domain.entity.VipPlan;
import com.sqx.app.vip.plan.db.service.IVipPlanService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class VipOrderServiceImpl implements VipOrderService {

    private static final String STATUS_PENDING = "pending_payment";
    private static final String STATUS_PAID = "paid";

    @Resource
    private VipOrderMapper vipOrderMapper;
    @Resource
    private IVipPlanService iVipPlanService;
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VipPurchaseVo createPurchase(Long userId, Long planId) {
        VipPlan plan = iVipPlanService.getById(planId);
        if (plan == null || plan.getAmount() == null || plan.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("会员套餐不存在或价格无效");
        }
        String orderNo = "VIP" + GenUtil.genOrderNo();
        VipOrder order = new VipOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setPlanName(plan.getName());
        order.setMonth(plan.getMonth());
        order.setAmount(plan.getAmount());
        order.setStatus(STATUS_PENDING);
        vipOrderMapper.insert(order);

        VipPurchaseVo vo = new VipPurchaseVo();
        vo.setOrderNo(orderNo);
        vo.setPlanId(plan.getId());
        vo.setPlanName(plan.getName());
        vo.setPayAmount(plan.getAmount());
        return vo;
    }

    @Override
    public BigDecimal preparePay(Long userId, String orderNo, BigDecimal amount) {
        VipOrder order = requirePendingOrder(userId, orderNo);
        if (amount != null && order.getAmount().compareTo(amount) != 0) {
            throw new BizException("会员订单金额不一致");
        }
        return order.getAmount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(String orderNo, String payChannel, String payTradeNo, BigDecimal amount) {
        VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .last("LIMIT 1"));
        if (order == null || STATUS_PAID.equals(order.getStatus())) {
            return;
        }
        int updated = vipOrderMapper.update(null, new LambdaUpdateWrapper<VipOrder>()
                .eq(VipOrder::getId, order.getId())
                .eq(VipOrder::getStatus, STATUS_PENDING)
                .set(VipOrder::getStatus, STATUS_PAID)
                .set(VipOrder::getPayChannel, payChannel)
                .set(VipOrder::getPayTradeNo, payTradeNo)
                .set(VipOrder::getPayTime, LocalDateTime.now()));
        if (updated == 0) {
            return;
        }
        UserEntity user = userMapper.selectById(order.getUserId());
        if (user != null) {
            UserEntity patch = new UserEntity();
            patch.setUserId(user.getUserId());
            patch.setIsVip(1);
            patch.setUserType(2);
            userMapper.updateById(patch);
        }
    }

    @Override
    public VipPayStatusVo payStatus(Long userId, String orderNo) {
        VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .eq(VipOrder::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("会员订单不存在");
        }
        VipPayStatusVo vo = new VipPayStatusVo();
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setPaid(STATUS_PAID.equals(order.getStatus()));
        return vo;
    }

    private VipOrder requirePendingOrder(Long userId, String orderNo) {
        VipOrder order = vipOrderMapper.selectOne(new LambdaQueryWrapper<VipOrder>()
                .eq(VipOrder::getOrderNo, orderNo)
                .eq(VipOrder::getUserId, userId)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BizException("会员订单不存在");
        }
        if (!Objects.equals(STATUS_PENDING, order.getStatus())) {
            throw new BizException("会员订单状态不可支付");
        }
        return order;
    }
}
