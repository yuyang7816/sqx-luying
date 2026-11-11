package com.sqx.app.base.pay.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.base.auth.auth.interceptor.AuthorizationInterceptor;
import com.sqx.app.base.constants.BaseConstant;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.pay.business.service.BusinessService;
import com.sqx.app.base.pay.domain.BusinessParam;
import com.sqx.app.base.pay.enums.BusinessType;
import com.sqx.app.base.pay.support.PayConfigSupport;
import com.sqx.app.base.utils.OaOAuthConfigUtil;
import com.sqx.app.base.utils.RequestUtil;
import com.sqx.common.core.domain.Result;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.pay.constants.PayConstant;
import com.sqx.common.pay.domain.UnifiedPayRequest;
import com.sqx.common.pay.enums.PayChannel;
import com.sqx.common.pay.enums.PayConfigCache;
import com.sqx.common.pay.enums.PayTradeType;
import com.sqx.common.pay.service.IPayService;
import com.sqx.common.redis.utils.ConfigUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;

/**
 * 统一支付接口
 */
@Slf4j
@Tag(name = "统一支付接口", description = "统一支付接口")
@RestController
@RequestMapping(BaseConstant.BASE_APP_PREFIX + "/pay")
public class PayController {

    private static final String NOTIFY_URL = BaseConstant.BASE_APP_PREFIX + "/pay/notify/%s";
    private static final String API_BASE_URL_KEY = "file_public_base_url";
    private static final String H5_DOMAIN_KEY = "sys_h5_domain";

    @Resource
    private IPayService iPayService;
    @Resource
    private BusinessService businessService;
    @Resource
    private PayConfigSupport payConfigSupport;

    @Login
    @Operation(description = "支付配置就绪检查")
    @GetMapping("config-check")
    public Result<Map<String, Object>> configCheck() {
        return Result.success(payConfigSupport.buildReadinessReport());
    }

    @Login
    @Operation(description = "统一支付")
    @PostMapping("pay")
    public Result<String> pay(@RequestBody BusinessParam param, HttpServletRequest request) {
        if (param == null) {
            throw new BizException("支付参数不能为空");
        }
        try {
            fillLoginUser(param, request);
            validatePayParam(param);
            businessService.process(param);
            UnifiedPayRequest payRequest = convertRequest(param, request);
            return Result.success(iPayService.unifiedPay(payRequest));
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("统一支付失败 orderNo={} businessType={} channel={}",
                    param.getOrderNo(), param.getBusinessType(), param.getChannel(), e);
            throw new BizException(resolvePayErrorMessage(e));
        }
    }

    private String resolvePayErrorMessage(Exception e) {
        if (e == null) {
            return "支付失败";
        }
        String message = StringUtils.defaultString(e.getMessage());
        Throwable cause = e.getCause();
        String causeMessage = cause == null ? "" : StringUtils.defaultString(cause.getMessage());
        String combined = message + " " + causeMessage;
        if (combined.contains("签名错误") || combined.contains("SIGNERROR")) {
            return "微信支付签名错误：请登录微信商户平台核对 API 密钥（V2，32 位）是否与后台 pay_wechat_mch_key 一致，"
                    + "并确认小程序 AppId（sys_wechat_jsapi_appid）已绑定到该商户号；修改后请在管理后台刷新系统配置并重启后端";
        }
        if (StringUtils.isNotBlank(message)) {
            return message;
        }
        if (StringUtils.isNotBlank(causeMessage)) {
            return causeMessage;
        }
        return "支付失败";
    }

    private void fillLoginUser(BusinessParam param, HttpServletRequest request) {
        Object userId = request.getAttribute(AuthorizationInterceptor.USER_KEY);
        if (userId instanceof Long) {
            param.setUserId((Long) userId);
            return;
        }
        if (userId instanceof Integer) {
            param.setUserId(((Integer) userId).longValue());
            return;
        }
        if (userId != null) {
            try {
                param.setUserId(Long.valueOf(String.valueOf(userId)));
            } catch (NumberFormatException e) {
                throw new BizException("登录状态异常，请重新登录");
            }
        }
    }

    private void validatePayParam(BusinessParam param) {
        if (StringUtils.isBlank(param.getBusinessType())) {
            throw new BizException("业务类型不能为空");
        }
        if (StringUtils.isBlank(param.getTradeType())) {
            throw new BizException("交易类型不能为空");
        }
        if (StringUtils.isBlank(param.getChannel())) {
            throw new BizException("支付渠道不能为空");
        }
        if (param.getAmount() == null) {
            throw new BizException("订单金额不能为空");
        }
        if (StringUtils.isBlank(param.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }
        if (StringUtils.isBlank(param.getRedirectUri())) {
            throw new BizException("支付成功后重定向地址不能为空");
        }
        if (Objects.equals(param.getChannel(), PayChannel.WX.getValue())
                && Objects.equals(StringUtils.defaultString(param.getWxAppScene()).trim(), "mp")
                && !OaOAuthConfigUtil.isEnabled()) {
            throw new BizException(AppError.OA_OAUTH_DISABLED);
        }
        if (Objects.equals(param.getChannel(), PayChannel.WX.getValue())
                && (Objects.equals(param.getTradeType(), PayTradeType.JSAPI.getCode())
                || Objects.equals(StringUtils.defaultString(param.getWxAppScene()).trim(), "ma")
                || Objects.equals(StringUtils.defaultString(param.getWxAppScene()).trim(), "mp"))
                && StringUtils.isBlank(param.getOpenId())) {
            throw new BizException("微信 JSAPI 支付需要先完成微信授权登录");
        }
    }

    private UnifiedPayRequest convertRequest(BusinessParam param, HttpServletRequest request) {
        PayChannel payChannel = PayChannel.fromCode(param.getChannel());
        UnifiedPayRequest.UnifiedPayRequestBuilder builder = UnifiedPayRequest.builder()
                .orderNo(StringUtils.isBlank(param.getOrderNo()) ? GenUtil.genOrderNo() : param.getOrderNo())
                .tradeType(param.getTradeType())
                .amount(param.getAmount())
                .channel(param.getChannel())
                .returnUrl(resolveReturnUrl(param.getRedirectUri()))
                .clientIp(RequestUtil.getClientIp())
                .openId(param.getOpenId())
                .notifyUrl(resolveNotifyUrl(payChannel, request));

        BusinessType businessType = BusinessType.fromCode(param.getBusinessType());
        builder.subject(businessType.getDesc());

        if (Objects.equals(payChannel, PayChannel.WX)) {
            builder.appId(getWxAppId(param));
        }

        return builder.build();
    }

    private String resolveNotifyUrl(PayChannel payChannel, HttpServletRequest request) {
        return resolveApiBaseUrl(request) + String.format(NOTIFY_URL, payChannel.getValue());
    }

    private String resolveApiBaseUrl(HttpServletRequest request) {
        String apiBase = ConfigUtil.getConfigValueOrDefault(API_BASE_URL_KEY);
        if (StringUtils.isNotBlank(apiBase)) {
            return StringUtils.removeEnd(apiBase.trim(), "/");
        }

        String h5Domain = ConfigUtil.getConfigValueOrDefault(H5_DOMAIN_KEY);
        if (StringUtils.isNotBlank(h5Domain)) {
            return StringUtils.removeEnd(h5Domain.trim(), "/") + "/sqx_fast";
        }

        String requestBase = buildRequestBaseUrl(request);
        if (StringUtils.isNotBlank(requestBase)) {
            log.warn("未配置 file_public_base_url，使用当前请求地址作为支付回调前缀: {}", requestBase);
            return requestBase;
        }

        throw new BizException("请配置支付回调域名 file_public_base_url");
    }

    private String buildRequestBaseUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String context = StringUtils.defaultString(request.getContextPath());
        boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        String portPart = defaultPort ? "" : (":" + port);
        return scheme + "://" + host + portPart + context;
    }

    private String resolveReturnUrl(String redirectUri) {
        if (StringUtils.isBlank(redirectUri)) {
            return redirectUri;
        }
        if (redirectUri.startsWith("http://") || redirectUri.startsWith("https://")) {
            return redirectUri;
        }

        String h5Domain = ConfigUtil.getConfigValueOrDefault(H5_DOMAIN_KEY);
        if (StringUtils.isBlank(h5Domain)) {
            return redirectUri;
        }

        String path = redirectUri.startsWith("/") ? redirectUri : ("/" + redirectUri);
        return StringUtils.removeEnd(h5Domain.trim(), "/") + "/#" + path;
    }

    private String getWxAppId(BusinessParam param) {
        String tradeType = param.getTradeType();
        String wxAppScene = StringUtils.defaultString(param.getWxAppScene()).trim();

        if (Objects.equals(tradeType, PayTradeType.APP.getCode()) || Objects.equals(wxAppScene, "app")) {
            return resolveConfigValue(PayConstant.Wx.User.APP_ID, PayConfigCache.WX_APP_ID.getCode());
        }

        if (Objects.equals(tradeType, PayTradeType.H5.getCode()) || Objects.equals(wxAppScene, "mp")) {
            return resolveConfigValue(PayConstant.Wx.User.MP_APP_ID, PayConfigCache.WX_H5_APP_ID.getCode());
        }

        if (Objects.equals(wxAppScene, "ma") || Objects.equals(tradeType, PayTradeType.JSAPI.getCode())) {
            return resolveConfigValue(PayConstant.Wx.User.MA_APP_ID, PayConfigCache.WX_JSAPI_APP_ID.getCode());
        }

        log.error("不支持的交易类型: tradeType={} wxAppScene={}", tradeType, wxAppScene);
        throw new BizException("不支持的交易类型");
    }

    private String resolveConfigValue(String primaryKey, String legacyKey) {
        String value = ConfigUtil.getConfigValue(primaryKey);
        if (StringUtils.isBlank(value)) {
            value = ConfigUtil.getConfigValue(legacyKey);
        }
        if (StringUtils.isBlank(value)) {
            throw new BizException("请配置 " + primaryKey);
        }
        return value.trim();
    }

    @Operation(hidden = true)
    @PostMapping("/notify/{channel}")
    public String handleNotify(HttpServletRequest request, @PathVariable String channel) {
        try {
            PayChannel payChannel = PayChannel.fromCode(channel);
            return iPayService.notify(request, payChannel.getValue());
        } catch (Exception e) {
            log.error("处理 {} 回调异常", channel, e);
            return "fail";
        }
    }
}
