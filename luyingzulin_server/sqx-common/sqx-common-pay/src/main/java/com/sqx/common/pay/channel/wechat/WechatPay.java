package com.sqx.common.pay.channel.wechat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.binarywang.wxpay.bean.request.BaseWxPayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayRefundRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.sqx.common.core.exception.BizException;
import com.github.binarywang.wxpay.bean.result.WxPayRefundResult;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderResult;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.sqx.common.core.exception.SqxException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.pay.channel.wechat.domain.WxPayRequest;
import com.sqx.common.pay.channel.wechat.domain.WxRefundRequest;
import com.sqx.common.pay.channel.wechat.enums.WxTradeType;
import com.sqx.common.pay.channel.wechat.manager.WxPayServiceManager;
import com.sqx.common.pay.channel.wechat.support.WxPayConfigResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chengming
 * @since 2025/4/18
 */
@Slf4j
@Component
public class WechatPay {

    @Resource
    private WxPayServiceManager wxPayServiceManager;

    /**
     * 拉起微信支付
     * @return 跳转链接
     */
    public String pay(WxTradeType tradeType, WxPayRequest request) throws WxPayException {
        WxPayConfigResolver.assertPayReady();
        WxPayService wxPayService = wxPayServiceManager.getWxPayService(request.getAppId());
        String wxTradeType = resolveWxTradeType(request.getTradeType());
        if ("JSAPI".equalsIgnoreCase(wxTradeType) && StringUtils.isBlank(request.getOpenId())) {
            throw new BizException("微信 JSAPI 支付需要先完成微信授权登录");
        }

        WxPayUnifiedOrderRequest orderRequest = new WxPayUnifiedOrderRequest();
        orderRequest.setBody(request.getSubject());
        orderRequest.setOutTradeNo(request.getOrderNo());
        orderRequest.setTotalFee(BaseWxPayRequest.yuanToFen(String.valueOf(request.getAmount()))); //元转成分
        orderRequest.setSpbillCreateIp(StringUtils.defaultIfBlank(request.getIp(), "127.0.0.1"));
        orderRequest.setNotifyUrl(request.getNotifyUrl());
        orderRequest.setTradeType(wxTradeType);
        orderRequest.setOpenid(request.getOpenId());
        if ("MWEB".equalsIgnoreCase(wxTradeType)) {
            orderRequest.setSceneInfo(buildMwebSceneInfo(
                    StringUtils.defaultIfBlank(request.getReturnUrl(), "https://www.example.com"),
                    StringUtils.defaultIfBlank(request.getSubject(), "露营租赁")
            ));
        }

        Object result = wxPayService.createOrder(orderRequest);
        return beanToJson(result);
    }

    private String buildMwebSceneInfo(String wapUrl, String wapName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            java.util.Map<String, String> h5Info = new java.util.LinkedHashMap<>();
            h5Info.put("type", "Wap");
            h5Info.put("wap_url", wapUrl);
            h5Info.put("wap_name", wapName);
            java.util.Map<String, Object> sceneInfo = new java.util.LinkedHashMap<>();
            sceneInfo.put("h5_info", h5Info);
            return objectMapper.writeValueAsString(sceneInfo);
        } catch (Exception e) {
            throw new SqxException("微信 H5 支付参数生成失败", e);
        }
    }

    private String resolveWxTradeType(String tradeType) {
        if (StringUtils.isBlank(tradeType)) {
            throw new BizException("交易类型不能为空");
        }
        if ("H5".equalsIgnoreCase(tradeType)) {
            return "MWEB";
        }
        return tradeType.trim().toUpperCase();
    }

    private String beanToJson(Object obj) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转 JSON 失败", e);
            throw new SqxException("支付参数生成失败", e);
        }
    }

    public String refund(WxRefundRequest request) throws WxPayException {
        WxPayService wxPayService = wxPayServiceManager.getWxPayService(request.getAppId());
        java.math.BigDecimal refundAmount = request.getRefundAmount() != null
                ? request.getRefundAmount() : request.getAmount();
        java.math.BigDecimal totalAmount = request.getAmount() != null ? request.getAmount() : refundAmount;

        WxPayRefundRequest wxPayRefundRequest = WxPayRefundRequest.newBuilder()
                .outRefundNo(StringUtils.defaultIfBlank(request.getRefundNo(), "RF" + GenUtil.genOrderNo()))
                .outTradeNo(request.getOrderNo())
                .refundFee(BaseWxPayRequest.yuan2Fen(refundAmount))
                .totalFee(BaseWxPayRequest.yuan2Fen(totalAmount))
                .refundDesc(request.getReason())
                .build();

        WxPayRefundResult result = wxPayService.refund(wxPayRefundRequest);
        return result.getRefundId();
    }
}
