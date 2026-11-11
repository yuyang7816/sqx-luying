package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.merchant.order.support.MerchantOrderSyncSupport;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.order.domain.bo.RentReturnApplyBo;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentReturnApply;
import com.sqx.app.rental.order.domain.vo.RentReturnDetailVo;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentReturnApplyMapper;
import com.sqx.app.rental.order.service.RentReturnService;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.order.support.RentOrderSupport;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 归还服务实现
 */
@Service
public class RentReturnServiceImpl implements RentReturnService {

    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentReturnApplyMapper rentReturnApplyMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private MerchantOrderSyncSupport merchantOrderSyncSupport;
    @Resource
    private com.sqx.app.rental.order.support.RentOrderNotificationSupport rentOrderNotificationSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyReturn(Long userId, RentReturnApplyBo applyBo) {
        PickupType.fromCode(applyBo.getReturnMethod());
        RentOrder order = RentOrderSupport.requireOwnedOrder(rentOrderMapper, userId, applyBo.getOrderId());
        RentOrderStateMachine.assertCanReturnApply(order.getStatus());
        String targetStatus = OrderStatus.RETURNING.getCode();
        RentOrderStateMachine.assertTransition(order.getStatus(), targetStatus);

        Long existing = rentReturnApplyMapper.selectCount(new LambdaQueryWrapper<RentReturnApply>()
                .eq(RentReturnApply::getOrderId, order.getId())
                .eq(RentReturnApply::getStatus, "pending"));
        if (existing != null && existing > 0) {
            throw new BizException("归还申请已提交，请勿重复操作");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, order.getId())
                .eq(RentOrder::getStatus, OrderStatus.RENTING.getCode())
                .set(RentOrder::getStatus, targetStatus)
                .set(RentOrder::getReturnTime, now));
        if (updated == 0) {
            throw new BizException("提交归还失败，订单状态已变更");
        }

        RentReturnApply apply = new RentReturnApply();
        apply.setOrderId(order.getId());
        apply.setOrderNo(order.getOrderNo());
        apply.setUserId(userId);
        apply.setReturnMethod(applyBo.getReturnMethod());
        apply.setReturnDate(applyBo.getReturnDate());
        apply.setRemark(applyBo.getRemark());
        apply.setImageUrls(joinImages(applyBo.getImageUrls()));
        apply.setStatus("pending");
        rentReturnApplyMapper.insert(apply);

        RentOrderSupport.writeOrderLog(rentOrderLogMapper, order.getId(), order.getOrderNo(),
                OrderStatus.RENTING.getCode(), targetStatus, "user", userId, "用户提交归还申请");

        RentOrder synced = rentOrderMapper.selectById(order.getId());
        if (synced != null) {
            merchantOrderSyncSupport.syncFromRentOrder(synced);
            rentOrderNotificationSupport.onReturnApply(synced);
        }
    }

    @Override
    public RentReturnDetailVo returnDetail(Long userId, Long orderId) {
        RentOrder order = RentOrderSupport.requireOwnedOrder(rentOrderMapper, userId, orderId);
        RentReturnApply apply = rentReturnApplyMapper.selectOne(new LambdaQueryWrapper<RentReturnApply>()
                .eq(RentReturnApply::getOrderId, orderId)
                .orderByDesc(RentReturnApply::getId)
                .last("LIMIT 1"));
        if (apply == null) {
            throw new BizException("暂无归还申请");
        }
        return toDetailVo(order, apply);
    }

    static RentReturnDetailVo toDetailVo(RentOrder order, RentReturnApply apply) {
        RentReturnDetailVo vo = new RentReturnDetailVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setReturnMethod(apply.getReturnMethod());
        vo.setReturnDate(apply.getReturnDate());
        vo.setRemark(apply.getRemark());
        vo.setImageUrls(splitImages(apply.getImageUrls()));
        vo.setApplyStatus(apply.getStatus());
        vo.setApplyTime(apply.getCreateTime());
        return vo;
    }

    private static String joinImages(List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return null;
        }
        return imageUrls.stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(","));
    }

    public static List<String> splitImages(String imageUrls) {
        if (StringUtils.isBlank(imageUrls)) {
            return Collections.emptyList();
        }
        return Arrays.stream(imageUrls.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
