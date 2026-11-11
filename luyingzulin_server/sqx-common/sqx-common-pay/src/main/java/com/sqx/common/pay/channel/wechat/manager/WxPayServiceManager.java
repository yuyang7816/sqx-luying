package com.sqx.common.pay.channel.wechat.manager;

import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.sqx.common.pay.channel.wechat.domain.WxPayConfig;
import com.sqx.common.pay.channel.wechat.service.WxPayConfigService;
import com.sqx.common.pay.channel.wechat.support.WxPayConfigResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chengming
 * @since 2026/1/9
 */
@Slf4j
@Component
public class WxPayServiceManager {

    @Resource
    private WxPayConfigService wxPayConfigService;

    private final Map<String, WxPayService> wxPayServiceMap = new ConcurrentHashMap<>();

    public void initializeWxPayServices() {
        List<WxPayConfig> wxPayConfigs = wxPayConfigService.getWxPayConfigs();

        for (WxPayConfig wxPayConfig : wxPayConfigs) {
            registerServiceIfPresent(wxPayConfig.getAppId(), "小程序");
            registerServiceIfPresent(wxPayConfig.getH5AppId(), "H5");
            registerServiceIfPresent(wxPayConfig.getJsAppId(), "公众号/JSAPI");
        }

        log.info("已初始化 {} 个微信支付客户端", wxPayServiceMap.size());
    }

    private void registerServiceIfPresent(String appId, String desc) {
        if (StringUtils.isNotBlank(appId)) {
            WxPayService service = createWxPayService(appId);
            wxPayServiceMap.put(appId, service);
            log.info("注册微信支付客户端: {} (appId={})", desc, appId);
        }
    }

    private WxPayService createWxPayService(String appId) {
        com.github.binarywang.wxpay.config.WxPayConfig config = new com.github.binarywang.wxpay.config.WxPayConfig();

        String mchId = WxPayConfigResolver.resolveMchId();
        String mchKey = WxPayConfigResolver.resolveMchKey();
        String keyPath = WxPayConfigResolver.resolveKeyPath();

        config.setAppId(StringUtils.trim(appId));
        config.setMchId(mchId);
        config.setMchKey(mchKey);
        if (StringUtils.isNotBlank(keyPath)) {
            config.setKeyPath(keyPath);
        }

        WxPayService service = new WxPayServiceImpl();
        service.setConfig(config);
        return service;
    }

    /**
     * 根据 appId 获取对应的微信支付客户端
     *
     * @param appId 微信 AppID（小程序/公众号/H5）
     * @return WxPayService 实例
     * @throws IllegalArgumentException 如果 appId 未配置或未初始化
     */
    public WxPayService getWxPayService(String appId) {
        WxPayService service = wxPayServiceMap.get(appId);
        if (service != null) {
            return service;
        }
        log.warn("未找到 appId={} 的微信支付客户端，尝试重新加载配置", appId);
        initializeWxPayServices();
        service = wxPayServiceMap.get(appId);
        if (service == null) {
            throw new IllegalArgumentException("未找到 appId=" + appId + " 对应的微信支付客户端，请检查微信支付配置并重启后端");
        }
        return service;
    }
}
