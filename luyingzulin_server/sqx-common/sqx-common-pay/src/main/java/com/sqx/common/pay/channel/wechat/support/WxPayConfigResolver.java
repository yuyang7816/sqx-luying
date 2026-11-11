package com.sqx.common.pay.channel.wechat.support;

import com.sqx.common.core.exception.BizException;
import com.sqx.common.pay.constants.PayConstant;
import com.sqx.common.pay.enums.PayConfigCache;
import com.sqx.common.redis.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 微信支付配置读取：兼容新旧 param_key，并去除首尾空白，避免签名错误。
 */
public final class WxPayConfigResolver {

    private WxPayConfigResolver() {
    }

    public static String resolveMchId() {
        return requireNonBlank(
                firstNonBlank(
                        safeConfig(PayConstant.Wx.MCH_ID),
                        safeConfig(PayConfigCache.WX_MCH_ID.getCode())
                ),
                "微信支付商户号"
        );
    }

    public static String resolveMchKey() {
        return requireNonBlank(
                firstNonBlank(
                        safeConfig(PayConstant.Wx.MCH_KEY),
                        safeConfig(PayConfigCache.WX_MCH_KEY.getCode())
                ),
                "微信支付商户 API 密钥（V2）"
        );
    }

    public static String resolveKeyPath() {
        return firstNonBlank(
                safeConfig(PayConstant.Wx.KEY_PATH),
                safeConfig(PayConfigCache.WX_KEY_PATH.getCode())
        );
    }

    public static String resolveMaAppId() {
        return firstNonBlank(
                safeConfig(PayConstant.Wx.User.MA_APP_ID),
                safeConfig(PayConfigCache.WX_JSAPI_APP_ID.getCode()),
                safeConfig("pay_wechat_jsapi_appid")
        );
    }

    public static String resolveMpAppId() {
        return firstNonBlank(
                safeConfig(PayConstant.Wx.User.MP_APP_ID),
                safeConfig(PayConfigCache.WX_H5_APP_ID.getCode()),
                safeConfig("pay_wechat_h5_appid")
        );
    }

    public static String resolveAppAppId() {
        return firstNonBlank(
                safeConfig(PayConstant.Wx.User.APP_ID),
                safeConfig(PayConfigCache.WX_APP_ID.getCode()),
                safeConfig("pay_wechat_app_appid")
        );
    }

    public static void assertPayReady() {
        String mchKey = resolveMchKey();
        if (mchKey.length() != 32) {
            throw new BizException("微信支付 API 密钥长度应为 32 位，请检查后台 pay_wechat_mch_key 是否为微信商户平台 V2 密钥");
        }
    }

    private static String requireNonBlank(String value, String label) {
        if (StringUtils.isBlank(value)) {
            throw new BizException("请配置" + label);
        }
        return value;
    }

    private static String safeConfig(String key) {
        try {
            return trimToNull(ConfigUtil.getConfigValueOrDefault(key));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String firstNonBlank(String... values) {
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
}
