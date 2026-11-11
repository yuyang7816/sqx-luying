package com.sqx.common.pay.channel.wechat.service;

import com.sqx.common.pay.channel.wechat.domain.WxPayConfig;
import com.sqx.common.pay.channel.wechat.support.WxPayConfigResolver;
import com.sqx.common.pay.constants.PayConstant;
import com.sqx.common.redis.utils.ConfigUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author chengming
 * @since 2026/1/9
 */
@Service
public class WxPayConfigService {

    private static final List<TerminalConfig> TERMINAL_CONFIGS = Collections.singletonList(
            new TerminalConfig("user", PayConstant.Wx.User.APP_ID, PayConstant.Wx.User.MA_APP_ID, PayConstant.Wx.User.MP_APP_ID)
    );

    public List<WxPayConfig> getWxPayConfigs() {
        String mchId = WxPayConfigResolver.resolveMchId();
        String mchKey = WxPayConfigResolver.resolveMchKey();
        String keyPath = WxPayConfigResolver.resolveKeyPath();

        List<WxPayConfig> wxPayConfigs = new ArrayList<>();
        for (TerminalConfig terminalConfig : TERMINAL_CONFIGS) {
            WxPayConfig wxPayConfig = new WxPayConfig();

            String appId = firstNonBlank(
                    safeConfig(terminalConfig.appIdKey),
                    WxPayConfigResolver.resolveAppAppId()
            );
            String maAppId = firstNonBlank(
                    safeConfig(terminalConfig.maAppIdKey),
                    WxPayConfigResolver.resolveMaAppId()
            );
            String mpAppId = firstNonBlank(
                    safeConfig(terminalConfig.mpAppIdKey),
                    WxPayConfigResolver.resolveMpAppId()
            );

            wxPayConfig.setAppId(appId);
            wxPayConfig.setJsAppId(maAppId);
            wxPayConfig.setH5AppId(mpAppId);
            wxPayConfig.setMchId(mchId);
            wxPayConfig.setMchKey(mchKey);
            wxPayConfig.setKeyPath(keyPath);

            wxPayConfigs.add(wxPayConfig);
        }

        return wxPayConfigs;
    }

    private static String safeConfig(String key) {
        try {
            String value = ConfigUtil.getConfigValueOrDefault(key);
            return value == null ? null : value.trim();
        } catch (Exception ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static class TerminalConfig {
        final String terminalName;
        final String appIdKey;
        final String maAppIdKey;
        final String mpAppIdKey;

        TerminalConfig(String name, String appId, String maAppId, String mpAppId) {
            this.terminalName = name;
            this.appIdKey = appId;
            this.maAppIdKey = maAppId;
            this.mpAppIdKey = mpAppId;
        }
    }
}
