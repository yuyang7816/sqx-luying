package com.sqx.common.pay.channel.wechat;

import com.github.binarywang.wxpay.exception.WxPayException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.pay.channel.wechat.domain.WxPayRequest;
import com.sqx.common.pay.channel.wechat.domain.WxRefundRequest;
import com.sqx.common.pay.channel.wechat.enums.WxTradeType;
import com.sqx.common.pay.domain.RefundRequest;
import com.sqx.common.pay.domain.UnifiedPayRequest;
import com.sqx.common.pay.enums.PayChannel;
import com.sqx.common.pay.notify.NotifyConstant;
import com.sqx.common.pay.operation.IPayOperation;
import org.springframework.stereotype.Component;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author chengming
 * @since 2026/1/9
 */
@Component
public class WxPayOperation implements IPayOperation {

    @Resource
    private WechatPay wechatPay;

    @Override
    public boolean support(String channel) {
        return Objects.equals(PayChannel.WX.getValue(), channel);
    }

    @Override
    public String pay(UnifiedPayRequest request) throws WxPayException {
        WxPayRequest wxReq = convertToWxPayRequest(request);
        return wechatPay.pay(WxTradeType.fromCode(request.getTradeType()), wxReq);
    }

    private WxPayRequest convertToWxPayRequest(UnifiedPayRequest request) {
        return WxPayRequest.builder()
                .appId(request.getAppId())
                .orderNo(request.getOrderNo())
                .subject(request.getSubject())
                .amount(request.getAmount())
                .tradeType(request.getTradeType())
                .notifyUrl(request.getNotifyUrl())
                .returnUrl(request.getReturnUrl())
                .ip(request.getClientIp())
                .openId(request.getOpenId())
                .build();
    }

    @Override
    public String refund(RefundRequest request) throws WxPayException {
        WxRefundRequest refundRequest = convertToWxRefundRequest(request);
        return wechatPay.refund(refundRequest);
    }

    private WxRefundRequest convertToWxRefundRequest(RefundRequest request) {
        java.math.BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount() : request.getAmount();
        return WxRefundRequest.builder()
                .appId(request.getAppId())
                .refundNo(StringUtils.defaultIfBlank(request.getRefundNo(), "RF" + GenUtil.genOrderNo()))
                .orderNo(request.getOrderNo())
                .amount(request.getAmount())
                .refundAmount(refundAmount)
                .reason(request.getReason())
                .build();
    }
}
