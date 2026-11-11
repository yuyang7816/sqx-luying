package com.sqx.app.rental.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.rental.enums.OrderStatus;
import com.sqx.app.rental.enums.PayBizType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.bo.RentRenewCreateBo;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.domain.entity.RentOrderPayment;
import com.sqx.app.rental.order.domain.entity.RentRenewRecord;
import com.sqx.app.rental.order.domain.vo.RentRenewCreateVo;
import com.sqx.app.rental.order.domain.vo.RentRenewPreviewVo;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
import com.sqx.app.rental.order.mapper.RentOrderLogMapper;
import com.sqx.app.rental.order.mapper.RentOrderMapper;
import com.sqx.app.rental.order.mapper.RentOrderPaymentMapper;
import com.sqx.app.rental.order.mapper.RentRenewRecordMapper;
import com.sqx.app.rental.order.service.RentRenewService;
import com.sqx.app.rental.order.support.RentOrderPriceCalculator;
import com.sqx.app.rental.order.support.RentOrderStateMachine;
import com.sqx.app.rental.order.support.RentOrderSupport;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 续租服务实现
 */
@Service
public class RentRenewServiceImpl implements RentRenewService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RentOrderMapper rentOrderMapper;
    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private RentRenewRecordMapper rentRenewRecordMapper;
    @Resource
    private RentOrderPaymentMapper rentOrderPaymentMapper;
    @Resource
    private RentOrderLogMapper rentOrderLogMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentStockService rentStockService;

    @Override
    public RentRenewPreviewVo preview(Long userId, Long orderId, String newEndTimeText) {
        LocalDateTime newEndTime = parseNewEndTime(newEndTimeText);
        RentOrder order = RentOrderSupport.requireOwnedOrder(rentOrderMapper, userId, orderId);
        RentOrderStateMachine.assertCanRenew(order.getStatus());

        LocalDateTime currentEndTime = order.getEndTime() == null ? LocalDateTime.now() : order.getEndTime();
        long extraHours = RentOrderPriceCalculator.resolveRenewBillingHours(currentEndTime, newEndTime);
        BigDecimal renewAmount = calcRenewAmountByHourlyRate(order, currentEndTime, newEndTime);
        BigDecimal hourUnitPrice = resolvePrimaryHourUnitPrice(order);

        RentRenewPreviewVo vo = new RentRenewPreviewVo();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setExtraHours((int) extraHours);
        vo.setHourUnitPrice(hourUnitPrice);
        vo.setCurrentEndTime(currentEndTime);
        vo.setNewEndTime(newEndTime);
        vo.setRenewAmount(renewAmount);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentRenewCreateVo create(Long userId, RentRenewCreateBo createBo) {
        RentRenewPreviewVo preview = preview(userId, createBo.getOrderId(), createBo.getNewEndTime());

        String renewNo = "RN" + GenUtil.genOrderNo();
        RentRenewRecord record = new RentRenewRecord();
        record.setRenewNo(renewNo);
        record.setOrderId(preview.getOrderId());
        record.setOrderNo(preview.getOrderNo());
        record.setUserId(userId);
        record.setExtraDays(preview.getExtraHours());
        record.setOldEndTime(preview.getCurrentEndTime());
        record.setNewEndTime(preview.getNewEndTime());
        record.setRenewAmount(preview.getRenewAmount());
        record.setPayStatus("pending");
        rentRenewRecordMapper.insert(record);

        RentRenewCreateVo vo = new RentRenewCreateVo();
        vo.setRenewNo(renewNo);
        vo.setOrderId(preview.getOrderId());
        vo.setRenewAmount(preview.getRenewAmount());
        vo.setPayStatus("pending");
        return vo;
    }

    @Override
    public BigDecimal prepareRenewPay(Long userId, String renewNo, BigDecimal requestAmount) {
        RentRenewRecord record = requirePendingRenew(renewNo);
        if (userId != null && !userId.equals(record.getUserId())) {
            throw new BizException("无权支付该续租单");
        }
        if (requestAmount != null && record.getRenewAmount() != null
                && requestAmount.compareTo(record.getRenewAmount()) != 0) {
            throw new BizException("支付金额与续租单不符");
        }
        return record.getRenewAmount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRenewPaySuccess(String renewNo, String payChannel, String payNo, BigDecimal paidAmount) {
        if (StringUtils.isBlank(renewNo)) {
            throw new BizException("续租单号不能为空");
        }
        RentRenewRecord record = rentRenewRecordMapper.selectOne(new LambdaQueryWrapper<RentRenewRecord>()
                .eq(RentRenewRecord::getRenewNo, renewNo)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BizException("续租单不存在");
        }
        if ("success".equals(record.getPayStatus())) {
            return;
        }
        if (!"pending".equals(record.getPayStatus())) {
            throw new BizException("续租单状态不可支付");
        }
        if (paidAmount != null && record.getRenewAmount() != null
                && paidAmount.compareTo(record.getRenewAmount()) != 0) {
            throw new BizException("支付金额与续租单不符");
        }

        RentOrder order = RentOrderSupport.requireOrder(rentOrderMapper, record.getOrderId());
        RentOrderStateMachine.assertCanRenew(order.getStatus());

        rentStockService.extendOccupyEndTime(order.getId(), record.getNewEndTime());

        LocalDateTime now = LocalDateTime.now();
        rentOrderMapper.update(null, new LambdaUpdateWrapper<RentOrder>()
                .eq(RentOrder::getId, order.getId())
                .eq(RentOrder::getStatus, OrderStatus.RENTING.getCode())
                .set(RentOrder::getEndTime, record.getNewEndTime())
                .set(RentOrder::getDays, RentOrderPriceCalculator.resolveDays(
                        order.getStartTime(), record.getNewEndTime(), null))
                .set(RentOrder::getRentAmount, order.getRentAmount().add(record.getRenewAmount())));

        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, order.getId()));
        for (RentOrderItem item : items) {
            rentOrderItemMapper.update(null, new LambdaUpdateWrapper<RentOrderItem>()
                    .eq(RentOrderItem::getId, item.getId())
                    .set(RentOrderItem::getEndTime, record.getNewEndTime())
                    .set(RentOrderItem::getDays, RentOrderPriceCalculator.resolveDays(
                            item.getStartTime(), record.getNewEndTime(), null)));
        }

        rentRenewRecordMapper.update(null, new LambdaUpdateWrapper<RentRenewRecord>()
                .eq(RentRenewRecord::getId, record.getId())
                .eq(RentRenewRecord::getPayStatus, "pending")
                .set(RentRenewRecord::getPayStatus, "success")
                .set(RentRenewRecord::getPayTime, now));

        RentOrderPayment payment = new RentOrderPayment();
        payment.setOrderId(order.getId());
        payment.setOrderNo(order.getOrderNo());
        payment.setPayNo(payNo);
        payment.setPayBizType(PayBizType.RENT_RENEW.getCode());
        payment.setAmount(record.getRenewAmount());
        payment.setPayChannel(payChannel);
        payment.setPayStatus("success");
        payment.setPayTime(now);
        rentOrderPaymentMapper.insert(payment);

        RentOrderSupport.writeOrderLog(rentOrderLogMapper, order.getId(), order.getOrderNo(),
                OrderStatus.RENTING.getCode(), OrderStatus.RENTING.getCode(),
                "system", null, "续租支付成功，延长至 " + DATE_TIME_FMT.format(record.getNewEndTime()));
    }

    private RentRenewRecord requirePendingRenew(String renewNo) {
        RentRenewRecord record = rentRenewRecordMapper.selectOne(new LambdaQueryWrapper<RentRenewRecord>()
                .eq(RentRenewRecord::getRenewNo, renewNo)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BizException("续租单不存在");
        }
        if (!"pending".equals(record.getPayStatus())) {
            throw new BizException("续租单状态不可支付");
        }
        return record;
    }

    private LocalDateTime parseNewEndTime(String newEndTimeText) {
        if (StringUtils.isBlank(newEndTimeText)) {
            throw new BizException("续租结束时间不能为空");
        }
        try {
            return LocalDateTime.parse(newEndTimeText.trim(), DATE_TIME_FMT);
        } catch (Exception ex) {
            throw new BizException("续租结束时间格式不正确");
        }
    }

    private BigDecimal calcRenewAmountByHourlyRate(RentOrder order, LocalDateTime extendStart, LocalDateTime newEndTime) {
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, order.getId()));
        BigDecimal total = BigDecimal.ZERO;
        for (RentOrderItem item : items) {
            RentGoods goods = rentGoodsMapper.selectById(item.getGoodsId());
            if (goods == null) {
                continue;
            }
            int qty = item.getQty() == null ? 1 : item.getQty();
            total = total.add(RentOrderPriceCalculator.calcRenewItemAmountByHourlyRate(
                    goods, extendStart, newEndTime, qty));
        }
        return total;
    }

    private BigDecimal resolvePrimaryHourUnitPrice(RentOrder order) {
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, order.getId()));
        for (RentOrderItem item : items) {
            RentGoods goods = rentGoodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                return RentOrderPriceCalculator.resolveHourlyUnitPrice(goods);
            }
        }
        return BigDecimal.ZERO;
    }
}
