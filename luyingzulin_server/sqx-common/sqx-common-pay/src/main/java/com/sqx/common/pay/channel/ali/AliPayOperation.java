package com.sqx.common.pay.channel.ali;

import com.alipay.api.AlipayApiException;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.pay.domain.RefundRequest;
import com.sqx.common.pay.domain.UnifiedPayRequest;
import com.sqx.common.pay.enums.PayChannel;
import com.sqx.common.pay.enums.PayTradeType;
import com.sqx.common.pay.operation.IPayOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 支付宝支付
 */
@Slf4j
@Component
public class AliPayOperation implements IPayOperation {

    @Resource
    private AliPay aliPay;

    @Override
    public boolean support(String channel) {
        return Objects.equals(PayChannel.ALI.getValue(), channel);
    }

    @Override
    public String pay(UnifiedPayRequest request) throws WxPayException, AlipayApiException {
        PayTradeType tradeType = PayTradeType.fromCode(request.getTradeType());
        if (tradeType == null) {
            log.error("不支持的支付宝交易类型: {}", request.getTradeType());
            throw new BizException("不支持的支付方式");
        }
        String amount = formatAmount(request.getAmount());
        String orderNo = request.getOrderNo();
        String subject = request.getSubject();
        String returnUrl = request.getReturnUrl();
        String notifyUrl = request.getNotifyUrl();

        try {
            if (PayTradeType.H5 == tradeType) {
                return aliPay.wapPay(orderNo, amount, subject, returnUrl, notifyUrl);
            }
            if (PayTradeType.APP == tradeType) {
                return aliPay.appPay(orderNo, amount, subject, returnUrl, notifyUrl);
            }
            if (PayTradeType.NATIVE == tradeType) {
                return aliPay.pagePay(orderNo, amount, subject, returnUrl, notifyUrl,
                        request.getQrPayMode(), request.getQrcodeWidth());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝下单失败 orderNo={} tradeType={}", orderNo, tradeType, e);
            throw new BizException(StringUtils.defaultIfBlank(e.getErrMsg(), "支付宝下单失败"));
        }

        log.error("不支持的支付宝交易类型: {}", tradeType);
        throw new BizException("不支持的支付方式");
    }

    @Override
    public String refund(RefundRequest request) throws WxPayException {
        try {
            String refundNo = StringUtils.defaultIfBlank(request.getRefundNo(), "RF" + GenUtil.genOrderNo());
            BigDecimal refundAmount = request.getRefundAmount() != null
                    ? request.getRefundAmount() : request.getAmount();
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("退款金额异常");
            }
            return aliPay.refund(
                    request.getOrderNo(),
                    refundNo,
                    formatAmount(refundAmount),
                    StringUtils.defaultIfBlank(request.getReason(), "订单退款")
            );
        } catch (AlipayApiException e) {
            log.error("支付宝退款失败 orderNo={}", request.getOrderNo(), e);
            throw new BizException(StringUtils.defaultIfBlank(e.getErrMsg(), "支付宝退款失败"));
        }
    }

    static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BizException("订单金额异常");
        }
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
