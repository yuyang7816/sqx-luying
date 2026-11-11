package com.sqx.app.base.utils.wx;

import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.utils.SysConfigUtil;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.lang3.StringUtils;

/**
 * 小程序 AppId / AppSecret 读取（对齐后台多处配置项）
 */
public final class WxMaConfigSupport {

    private WxMaConfigSupport() {
    }

    public static MaCredentials resolveCredentials() {
        String maAppId = readConfig(SysConfigConstant.Sys.SYS_USER_MA_APPID);
        String maSecret = readConfig(SysConfigConstant.Sys.SYS_USER_MA_SECRET);
        String jsapiAppId = readConfig("sys_wechat_jsapi_appid");
        String jsapiSecret = readConfig("sys_wechat_jsapi_secret");
        String payJsapiAppId = readConfig("pay_wx_jsapi_app_id");

        String appId = firstNonBlank(maAppId, jsapiAppId, payJsapiAppId);
        String secret = resolveSecret(appId, maAppId, maSecret, jsapiAppId, jsapiSecret);
        return new MaCredentials(appId, secret);
    }

    /**
     * 同一小程序在后台可能配置在 sys_user_ma_* 或 sys_wechat_jsapi_*。
     * 当 AppId 一致且 jsapi Secret 已填写时，优先使用 jsapi Secret，避免旧 ma_secret 占位值导致登录失败。
     */
    private static String resolveSecret(String appId, String maAppId, String maSecret,
                                        String jsapiAppId, String jsapiSecret) {
        if (StringUtils.isNotBlank(jsapiSecret) && appIdsCompatible(appId, maAppId, jsapiAppId)) {
            return jsapiSecret;
        }
        return firstNonBlank(maSecret, jsapiSecret);
    }

    private static boolean appIdsCompatible(String appId, String maAppId, String jsapiAppId) {
        if (StringUtils.isBlank(appId)) {
            return true;
        }
        if (StringUtils.isNotBlank(maAppId) && !appId.equals(maAppId)) {
            return false;
        }
        if (StringUtils.isNotBlank(jsapiAppId) && !appId.equals(jsapiAppId)) {
            return false;
        }
        return true;
    }

    private static String readConfig(String key) {
        try {
            return StringUtils.trimToEmpty(SysConfigUtil.getConfigValue(key));
        } catch (BizException ex) {
            return "";
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    public static final class MaCredentials {
        private final String appId;
        private final String secret;

        public MaCredentials(String appId, String secret) {
            this.appId = appId;
            this.secret = secret;
        }

        public String getAppId() {
            return appId;
        }

        public String getSecret() {
            return secret;
        }
    }
}
