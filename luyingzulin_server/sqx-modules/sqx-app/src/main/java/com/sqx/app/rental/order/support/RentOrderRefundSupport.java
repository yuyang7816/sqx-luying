package com.sqx.app.rental.order.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderPayment;
import com.sqx.app.rental.order.mapper.RentOrderPaymentMapper;
import com.sqx.app.wallet.transaction.db.service.IUserWalletTransactionService;
import com.sqx.app.wallet.transaction.domain.entity.UserWalletTransaction;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.pay.domain.RefundRequest;
import com.sqx.common.pay.enums.PayChannel;
import com.sqx.common.pay.enums.PayConfigCache;
import com.sqx.common.pay.service.IPayService;
import com.sqx.common.redis.utils.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 租赁订单原路退款（微信/支付宝/余额）
 */
@Slf4j
@Component
public class RentOrderRefundSupport {

    private static final String PAY_TYPE_BALANCE = "balance";
    private static final int TX_TYPE_REFUND = 3;
    private static final int TX_DIRECTION_IN = 1;
    private static final int TX_STATUS_DONE = 2;

    @Resource
    private RentOrderPaymentMapper rentOrderPaymentMapper;
    @Resource
    private IUserWalletService iUserWalletService;
    @Resource
    private IUserWalletTransactionService iUserWalletTransactionService;
    @Resource
    private IPayService iPayService;

    public void refundPaidOrder(RentOrder order, String reason) {
        if (order == null || StringUtils.isBlank(order.getOrderNo())) {
            return;
        }
        RentOrderPayment payment = findSuccessPayment(order.getOrderNo());
        if (payment == null) {
            log.warn("租赁订单无成功支付记录，跳过退款: orderNo={}", order.getOrderNo());
            return;
        }
        if (isAlreadyRefunded(order.getId())) {
            log.info("租赁订单已退款，跳过: orderNo={}", order.getOrderNo());
            return;
        }
        BigDecimal refundAmount = payment.getAmount();
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        refundByChannel(order, payment, refundAmount, reason);
    }

    private RentOrderPayment findSuccessPayment(String orderNo) {
        return rentOrderPaymentMapper.selectOne(new LambdaQueryWrapper<RentOrderPayment>()
                .eq(RentOrderPayment::getOrderNo, orderNo)
                .eq(RentOrderPayment::getPayStatus, "success")
                .orderByDesc(RentOrderPayment::getId)
                .last("LIMIT 1"));
    }

    private boolean isAlreadyRefunded(Long orderId) {
        return iUserWalletTransactionService.count(new LambdaQueryWrapper<UserWalletTransaction>()
                .eq(UserWalletTransaction::getBizId, orderId)
                .eq(UserWalletTransaction::getTransactionType, TX_TYPE_REFUND)
                .eq(UserWalletTransaction::getStatus, TX_STATUS_DONE)) > 0;
    }

    private void refundByChannel(RentOrder order, RentOrderPayment payment, BigDecimal refundAmount, String reason) {
        String channel = StringUtils.trimToEmpty(payment.getPayChannel());
        if (Objects.equals(channel, PAY_TYPE_BALANCE)) {
            refundToWallet(order, refundAmount, reason, PAY_TYPE_BALANCE);
            return;
        }
        String payChannel = resolveRefundChannel(channel);
        if (payChannel != null) {
            refundViaChannel(order, payment, refundAmount, reason, payChannel);
            return;
        }
        throw new BizException("无法识别支付方式，请联系客服处理退款");
    }

    private String resolveRefundChannel(String payType) {
        if (Objects.equals(payType, PayChannel.WX.getValue()) || Objects.equals(payType, "wechat")) {
            return PayChannel.WX.getValue();
        }
        if (Objects.equals(payType, PayChannel.ALI.getValue()) || Objects.equals(payType, "alipay")) {
            return PayChannel.ALI.getValue();
        }
        return null;
    }

    private void refundToWallet(RentOrder order, BigDecimal refundAmount, String reason, String channel) {
        ensureWalletExists(order.getUserId());
        iUserWalletService.incr(order.getUserId(), refundAmount);
        saveRefundTransaction(order, refundAmount, reason, channel);
        log.info("租赁订单余额退款成功: orderNo={}, userId={}, amount={}", order.getOrderNo(), order.getUserId(), refundAmount);
    }

    private void refundViaChannel(RentOrder order, RentOrderPayment payment, BigDecimal refundAmount, String reason, String channel) {
        RefundRequest request = new RefundRequest();
        request.setChannel(channel);
        request.setOrderNo(payment.getOrderNo());
        request.setAmount(payment.getAmount());
        request.setRefundAmount(refundAmount);
        request.setRefundNo("RF" + GenUtil.genOrderNo());
        request.setReason(StringUtils.defaultIfBlank(reason, "租赁订单退款"));
        if (Objects.equals(channel, PayChannel.WX.getValue())) {
            request.setAppId(resolveWxAppId());
        }
        try {
            iPayService.refund(request);
            saveRefundTransaction(order, refundAmount, reason, channel);
            log.info("租赁订单渠道退款成功: orderNo={}, channel={}, amount={}", order.getOrderNo(), channel, refundAmount);
        } catch (Exception e) {
            log.error("租赁订单渠道退款失败: orderNo={}, channel={}", order.getOrderNo(), channel, e);
            throw new BizException("退款失败：" + StringUtils.defaultIfBlank(e.getMessage(), "支付渠道异常"));
        }
    }

    private void saveRefundTransaction(RentOrder order, BigDecimal refundAmount, String reason, String channel) {
        UserWalletTransaction transaction = new UserWalletTransaction();
        transaction.setUserId(order.getUserId());
        transaction.setAmount(refundAmount);
        transaction.setTransactionType(TX_TYPE_REFUND);
        transaction.setDirection(TX_DIRECTION_IN);
        transaction.setBizId(order.getId());
        transaction.setStatus(TX_STATUS_DONE);
        transaction.setTitle("租赁订单退款");
        transaction.setDescription(StringUtils.defaultIfBlank(reason, "租赁订单退款"));
        transaction.setExtraData(String.format("{\"orderNo\":\"%s\",\"payType\":\"%s\"}", order.getOrderNo(), channel));
        iUserWalletTransactionService.save(transaction);
    }

    private void ensureWalletExists(Long userId) {
        UserWallet wallet = iUserWalletService.getOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId)
                .last("limit 1"));
        if (wallet != null) {
            return;
        }
        UserWallet newWallet = new UserWallet();
        newWallet.setUserId(userId);
        newWallet.setAmount(BigDecimal.ZERO);
        iUserWalletService.save(newWallet);
    }

    private String resolveWxAppId() {
        String appId = ConfigUtil.getConfigValue(PayConfigCache.WX_H5_APP_ID.getCode());
        if (StringUtils.isNotBlank(appId)) {
            return appId;
        }
        appId = ConfigUtil.getConfigValue(PayConfigCache.WX_JSAPI_APP_ID.getCode());
        if (StringUtils.isNotBlank(appId)) {
            return appId;
        }
        return ConfigUtil.getConfigValue(PayConfigCache.WX_APP_ID.getCode());
    }
}
