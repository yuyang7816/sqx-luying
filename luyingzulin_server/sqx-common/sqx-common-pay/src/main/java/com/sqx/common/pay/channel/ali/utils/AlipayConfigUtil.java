package com.sqx.common.pay.channel.ali.utils;

import com.alipay.api.*;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.pay.channel.ali.constants.AlipayConstant;
import com.sqx.common.pay.channel.ali.enums.AliMode;
import com.sqx.common.pay.channel.ali.enums.AliSandbox;
import com.sqx.common.pay.enums.PayConfigCache;
import com.sqx.common.redis.utils.ConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author chengming
 * @since 2025/4/18
 */
@Slf4j
public class AlipayConfigUtil {

    /**
     * 支付宝客户端
     * @return
     * @throws AlipayApiException
     */
    public static AlipayClient getAlipayClient() throws AlipayApiException {
        AlipayConfig config = new AlipayConfig();

        //设置网关地址
        config.setServerUrl(AlipayConstant.SERVER_URL);
        //设置请求格式，固定值json
        config.setFormat(AlipayConstants.FORMAT_JSON);
        //设置字符集
        config.setCharset(AlipayConstants.CHARSET_UTF8);
        //设置签名类型
        config.setSignType(AlipayConstants.SIGN_TYPE_RSA2);

        String appId = ConfigUtil.getConfigValue(PayConfigCache.ALI_APPID.getCode());
        if (StringUtils.isBlank(appId)) {
            throw new BizException("请配置支付宝 AppId (pay_ali_appid)");
        }
        config.setAppId(appId.trim());
        String privateKey = ConfigUtil.getConfigValue(PayConfigCache.ALI_PRIVATE_KEY.getCode());
        if (StringUtils.isBlank(privateKey)) {
            throw new BizException("请配置支付宝私钥 (pay_ali_private_key)");
        }
        config.setPrivateKey(privateKey.trim());

        // 是否为沙箱模式
        String sandbox = ConfigUtil.getConfigValue(PayConfigCache.ALI_SANDBOX.getCode());
        if (Objects.equals(sandbox, AliSandbox.YES.getCode())) {
            config.setServerUrl(AlipayConstant.SANDBOX_SERVER_URL);
        }

        // 是否开启证书模式
        String aliMode = ConfigUtil.getConfigValue(PayConfigCache.ALI_MODE.getCode());
        if (Objects.equals(AliMode.PUBLIC_KEY.getCode(), aliMode)) {
            String publicKey = ConfigUtil.getConfigValue(PayConfigCache.ALI_PUBLIC_KEY.getCode());
            if (StringUtils.isBlank(publicKey)) {
                throw new BizException("请配置支付宝公钥 (pay_ali_public_key)");
            }
            config.setAlipayPublicKey(publicKey.trim());
        } else if (Objects.equals(AliMode.CERT.getCode(), aliMode)) {
            String appCertPath = ConfigUtil.getConfigValue(PayConfigCache.ALI_APP_CERT_PATH.getCode());
            config.setAppCertPath(appCertPath);

            String alipayPublicCertPath = ConfigUtil.getConfigValue(PayConfigCache.ALI_ALIPAY_PUBLIC_CERT_PATH.getCode());
            config.setAlipayPublicCertPath(alipayPublicCertPath);

            String rootCertPath = ConfigUtil.getConfigValue(PayConfigCache.ALI_ROOT_CERT_PATH.getCode());
            config.setRootCertPath(rootCertPath);
        } else {
            log.error("支付宝支付模式配置错误, 当前配置: {}", aliMode);
            throw new BizException("请配置支付宝支付模式 pay_ali_mode（1=公钥模式 2=证书模式）");
        }

        return new DefaultAlipayClient(config);
    }
}
