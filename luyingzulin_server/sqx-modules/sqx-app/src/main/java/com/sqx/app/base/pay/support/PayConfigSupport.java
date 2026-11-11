package com.sqx.app.base.pay.support;

import com.sqx.app.base.constants.BaseConstant;
import com.sqx.common.pay.channel.ali.enums.AliMode;
import com.sqx.common.pay.constants.PayConstant;
import com.sqx.common.pay.enums.PayConfigCache;
import com.sqx.common.redis.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 支付配置就绪检查（不暴露密钥内容）
 */
@Component
public class PayConfigSupport {

    private static final String API_BASE_URL_KEY = "file_public_base_url";
    private static final String H5_DOMAIN_KEY = "sys_h5_domain";
    private static final String NOTIFY_PATH = BaseConstant.BASE_APP_PREFIX + "/pay/notify/";

    public Map<String, Object> buildReadinessReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("alipay", buildAlipayReport());
        report.put("wechat", buildWechatReport());
        report.put("notifyUrls", buildNotifyUrls());
        return report;
    }

    private Map<String, Object> buildAlipayReport() {
        Map<String, Object> alipay = new LinkedHashMap<>();
        String appId = safeConfig(PayConfigCache.ALI_APPID.getCode());
        String privateKey = safeConfig(PayConfigCache.ALI_PRIVATE_KEY.getCode());
        String mode = safeConfig(PayConfigCache.ALI_MODE.getCode());
        String sandbox = safeConfig(PayConfigCache.ALI_SANDBOX.getCode());
        boolean publicKeyReady = StringUtils.isNotBlank(safeConfig(PayConfigCache.ALI_PUBLIC_KEY.getCode()));
        boolean certReady = StringUtils.isNoneBlank(
                safeConfig(PayConfigCache.ALI_APP_CERT_PATH.getCode()),
                safeConfig(PayConfigCache.ALI_ALIPAY_PUBLIC_CERT_PATH.getCode()),
                safeConfig(PayConfigCache.ALI_ROOT_CERT_PATH.getCode())
        );
        boolean modeReady = Objects.equals(mode, AliMode.PUBLIC_KEY.getCode()) ? publicKeyReady
                : Objects.equals(mode, AliMode.CERT.getCode()) ? certReady : false;

        alipay.put("configured", StringUtils.isNoneBlank(appId, privateKey) && modeReady);
        alipay.put("appId", mask(appId));
        alipay.put("mode", Objects.equals(mode, AliMode.CERT.getCode()) ? "cert" : "public_key");
        alipay.put("sandbox", Objects.equals(sandbox, "1"));
        if (!modeReady) {
            alipay.put("hint", Objects.equals(mode, AliMode.CERT.getCode())
                    ? "请配置 pay_ali_app_cert_path / pay_ali_alipay_public_cert_path / pay_ali_root_cert_path"
                    : "请配置 pay_ali_public_key");
        }
        return alipay;
    }

    private Map<String, Object> buildWechatReport() {
        Map<String, Object> wechat = new LinkedHashMap<>();
        String mchId = firstNonBlank(
                safeConfig(PayConstant.Wx.MCH_ID),
                safeConfig(PayConfigCache.WX_MCH_ID.getCode())
        );
        String mchKey = firstNonBlank(
                safeConfig(PayConstant.Wx.MCH_KEY),
                safeConfig(PayConfigCache.WX_MCH_KEY.getCode())
        );
        String keyPath = firstNonBlank(
                safeConfig(PayConstant.Wx.KEY_PATH),
                safeConfig(PayConfigCache.WX_KEY_PATH.getCode())
        );
        String maAppId = firstNonBlank(
                safeConfig(PayConstant.Wx.User.MA_APP_ID),
                safeConfig(PayConfigCache.WX_JSAPI_APP_ID.getCode())
        );
        String mpAppId = firstNonBlank(
                safeConfig(PayConstant.Wx.User.MP_APP_ID),
                safeConfig(PayConfigCache.WX_H5_APP_ID.getCode())
        );
        String appAppId = firstNonBlank(
                safeConfig(PayConstant.Wx.User.APP_ID),
                safeConfig(PayConfigCache.WX_APP_ID.getCode())
        );

        wechat.put("configured", StringUtils.isNoneBlank(mchId, mchKey, keyPath)
                && StringUtils.isNoneBlank(maAppId, mpAppId, appAppId));
        wechat.put("mchId", mask(mchId));
        wechat.put("miniAppId", mask(maAppId));
        wechat.put("mpAppId", mask(mpAppId));
        wechat.put("appAppId", mask(appAppId));
        wechat.put("certConfigured", StringUtils.isNotBlank(keyPath));
        if (StringUtils.isBlank(keyPath)) {
            wechat.put("hint", "请配置 sys_wx_key_path（微信商户 p12 证书路径）");
        }
        return wechat;
    }

    private Map<String, Object> buildNotifyUrls() {
        Map<String, Object> notify = new LinkedHashMap<>();
        String apiBase = firstNonBlank(
                safeConfig(API_BASE_URL_KEY),
                buildH5ApiBase()
        );
        notify.put("apiBaseConfigured", StringUtils.isNotBlank(apiBase));
        notify.put("apiBase", StringUtils.defaultString(apiBase));
        if (StringUtils.isNotBlank(apiBase)) {
            String base = StringUtils.removeEnd(apiBase.trim(), "/");
            notify.put("alipayNotifyUrl", base + NOTIFY_PATH + "ali");
            notify.put("wechatNotifyUrl", base + NOTIFY_PATH + "wx");
        } else {
            notify.put("hint", "请配置 file_public_base_url（含 /sqx_fast 的公网 API 前缀）");
        }
        return notify;
    }

    private String buildH5ApiBase() {
        String h5Domain = safeConfig(H5_DOMAIN_KEY);
        if (StringUtils.isBlank(h5Domain)) {
            return null;
        }
        return StringUtils.removeEnd(h5Domain.trim(), "/") + "/sqx_fast";
    }

    private String safeConfig(String key) {
        try {
            return ConfigUtil.getConfigValueOrDefault(key);
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String mask(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 6) {
            return trimmed;
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 3);
    }
}
