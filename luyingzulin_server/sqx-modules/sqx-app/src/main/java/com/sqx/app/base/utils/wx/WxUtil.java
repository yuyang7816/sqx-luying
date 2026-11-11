package com.sqx.app.base.utils.wx;

import cn.hutool.core.codec.Base64;
import com.alibaba.fastjson2.JSONObject;
import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.enums.ClientEnum;
import com.sqx.app.base.utils.SysConfigUtil;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.http.OkHttpUtil;
import com.sqx.common.redis.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.AlgorithmParameterSpec;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author chengming
 * @since 2025/8/5
 */
@Component
@Slf4j
public class WxUtil {
    /**
     * 微信小程序登录
     *
     * @param appId   appId
     * @param secret  secret
     * @param js_code code
     */
    public WxJsLoginRes wxJsLogin(String appId, String secret, String js_code) {
        if (StringUtils.isBlank(appId)) {
            throw new BizException("小程序 AppId 未配置，请在系统配置中设置 sys_user_ma_appid");
        }
        if (StringUtils.isBlank(secret)) {
            throw new BizException("小程序 Secret 未配置，请在系统配置中设置 sys_user_ma_secret");
        }
        if (StringUtils.isBlank(js_code)) {
            throw new BizException("微信登录凭证不能为空");
        }

        String api = "https://api.weixin.qq.com/sns/jscode2session";

        Map<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", secret);
        params.put("js_code", js_code);
        params.put("grant_type", "authorization_code");

        String resp = OkHttpUtil.doGet(api, params, null);
        if (StringUtils.isBlank(resp)) {
            throw new BizException("微信登录接口无响应，请稍后重试");
        }

        WxJsLoginRes wxJsLoginRes = JSONObject.parseObject(resp, WxJsLoginRes.class);
        if (wxJsLoginRes == null) {
            throw new BizException("微信登录接口返回异常，请稍后重试");
        }
        if (Objects.nonNull(wxJsLoginRes.getErrcode()) && wxJsLoginRes.getErrcode() != 0) {
            log.error("微信小程序登录失败 appId={} errcode={} errmsg={}", appId, wxJsLoginRes.getErrcode(), wxJsLoginRes.getErrmsg());
            throw new BizException(mapWxLoginError(appId, wxJsLoginRes));
        }
        if (StringUtils.isBlank(wxJsLoginRes.getOpenid())) {
            throw new BizException("微信登录未返回 openid，请稍后重试");
        }

        return wxJsLoginRes;
    }

    private String mapWxLoginError(String appId, WxJsLoginRes wxJsLoginRes) {
        Integer errcode = wxJsLoginRes.getErrcode();
        if (errcode == null) {
            return StringUtils.defaultIfBlank(wxJsLoginRes.getErrmsg(), "微信登录失败，请稍后重试");
        }
        switch (errcode) {
            case 40029:
            case 40163:
                return "微信登录凭证无效或已使用，请重新点击登录";
            case 40125:
            case 41004:
                return "小程序 AppSecret 无效，请在管理后台核对 sys_user_ma_secret / sys_wechat_jsapi_secret 是否与微信公众平台 AppSecret 一致（AppId="
                        + maskAppId(appId) + "）";
            case 40013:
                return "小程序 AppId 配置错误（当前 AppId=" + maskAppId(appId) + "），请检查后台 sys_user_ma_appid 是否为 wx3c3a5eb165c4fcba";
            default:
                return StringUtils.defaultIfBlank(wxJsLoginRes.getErrmsg(), "微信登录失败，请稍后重试");
        }
    }

    private String maskAppId(String appId) {
        if (StringUtils.isBlank(appId)) {
            return "未配置";
        }
        String trimmed = appId.trim();
        if (trimmed.length() <= 8) {
            return trimmed;
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * 公众号 OAuth2：code 换取 access_token 与 openid
     */
    public WxMpOAuthRes wxMpOAuthAccessToken(String appId, String secret, String code) {
        String api = "https://api.weixin.qq.com/sns/oauth2/access_token";

        Map<String, String> params = new HashMap<>();
        params.put("appid", appId);
        params.put("secret", secret);
        params.put("code", code);
        params.put("grant_type", "authorization_code");

        String resp = OkHttpUtil.doGet(api, params, null);
        WxMpOAuthRes oauthRes = JSONObject.parseObject(resp, WxMpOAuthRes.class);
        if (Objects.nonNull(oauthRes.getErrcode()) && oauthRes.getErrcode() != 0) {
            log.error("公众号 OAuth 返回结果: {}", oauthRes);
            throw new BizException("公众号授权登录失败");
        }
        return oauthRes;
    }

    /**
     * 解密手机号
     *
     * @param decryptData 加密手机号（微信返回）
     * @param key         session_key
     * @param iv          iv（微信返回）
     */
    public String decryptS5(String decryptData, String key, String iv) {
        try {
            byte[] encData = Base64.decode(decryptData);
            byte[] ivs = Base64.decode(iv);
            byte[] keys = Base64.decode(key);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivs);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keys, "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return new String(cipher.doFinal(encData));
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException("解密手机号失败");
        }
    }

    /**
     * 该接口用于将code换取用户手机号
     * 说明，每个code只能使用一次，code的有效期为5min
     *
     * <a href="https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-info/phone-number/getPhoneNumber.html">参考文档</a>
     *
     * @param code   code
     * @param openId openId
     * @return
     */
    public WxUserPhone getUserPhoneNumber(String code, String openId, String clientType) {
        String api = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=";

        String accessToken = getToken(clientType);
        api = api + accessToken;

        Map<String, String> body = new HashMap<>();
        body.put("openid", openId);
        body.put("code", code);
        String resp = OkHttpUtil.doPost(api, JSONObject.toJSONString(body));
        log.info("【获取用户手机号】微信返回值: {}", resp);
        JSONObject respJson = JSONObject.parseObject(resp);
        Integer errcode = respJson.getInteger("errcode");
        if (Objects.isNull(errcode) || 0 != errcode) {
            log.error("【获取用户手机号】失败, 错误信息: {}", respJson.getString("errmsg"));
            throw new BizException("获取手机号失败");
        }

        JSONObject phoneInfo = respJson.getJSONObject("phone_info");

        WxUserPhone wxUserPhone = new WxUserPhone();
        wxUserPhone.setPhoneNumber(phoneInfo.getString("phoneNumber"));
        wxUserPhone.setPurePhoneNumber(phoneInfo.getString("purePhoneNumber"));
        wxUserPhone.setCountryCode(phoneInfo.getString("countryCode"));

        return wxUserPhone;
    }

    private String getStableAccessToken(String appId, String appSecret) {
        String cacheKey = "WX::ACCESS_TOKEN::" + appId;
        if (RedisUtil.hasKey(cacheKey)) {
            return RedisUtil.getCacheObject(cacheKey);
        }

        String api = "https://api.weixin.qq.com/cgi-bin/stable_token";

        Map<String, String> body = new HashMap<>();
        body.put("appid", appId);
        body.put("secret", appSecret);
        body.put("grant_type", "client_credential");

        String resp = OkHttpUtil.doPost(api, JSONObject.toJSONString(body));
        log.info("【获取微信access_token】微信返回值: {}", resp);

        JSONObject respJson = JSONObject.parseObject(resp);
        String accessToken = respJson.getString("access_token");
        if (Objects.isNull(accessToken)) {
            throw new BizException("【获取微信access_token】失败");
        }

        Integer expiresIn = respJson.getInteger("expires_in");
        RedisUtil.setCacheObject(cacheKey, accessToken, Duration.ofSeconds(expiresIn));

        return accessToken;
    }

    public String getToken(String clientType) {
        if (Objects.equals(clientType, ClientEnum.USER.getCode())) {
            WxMaConfigSupport.MaCredentials credentials = WxMaConfigSupport.resolveCredentials();
            return getStableAccessToken(credentials.getAppId(), credentials.getSecret());
        }

        log.error("不支持的客户端：[{}]", clientType);
        throw new BizException("不支持的客户端");
    }

    /**
     * 发送订阅消息
     */
    public void sendSubscribeMessage(WxMaSubscribeMessage message, String clientType) {
        String accessToken = getToken(clientType);

        String api = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("touser", message.getToUser());
        body.put("template_id", message.getTemplateId());

        Map<String, Map<String, String>> jsonMap = new HashMap<>();
        for (WxMaSubscribeMessage.MsgData item : message.getData()) {
            jsonMap.put(item.getName(), Collections.singletonMap("value", item.getValue()));
        }

        body.put("data", jsonMap);

        String resp = OkHttpUtil.doPost(api, JSONObject.toJSONString(body));
        JSONObject respJson = JSONObject.parseObject(resp);
        Integer errcode = respJson.getInteger("errcode");
        if (Objects.isNull(errcode) || 0 != errcode) {
            log.error("小程序消息推送失败: [{}]", respJson.getString("errmsg"));
            throw new BizException("小程序消息推送失败");
        }
    }

    /**
     * 生成小程序无限量二维码（用于邀请海报）
     *
     * @param scene 场景值，最长 32 字符
     * @param page  小程序页面路径（不含开头 /）
     */
    public String getWxUnlimitedQrCodeBase64(String scene, String page) {
        WxMaConfigSupport.MaCredentials credentials = WxMaConfigSupport.resolveCredentials();
        String accessToken = getStableAccessToken(credentials.getAppId(), credentials.getSecret());
        String api = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("scene", scene);
        body.put("page", page);
        body.put("check_path", false);
        body.put("width", 430);

        byte[] bytes = OkHttpUtil.doPostBytes(api, JSONObject.toJSONString(body), "application/json", null);
        if (bytes.length > 0 && bytes[0] == '{') {
            JSONObject err = JSONObject.parseObject(new String(bytes));
            log.error("生成小程序码失败: {}", err);
            throw new BizException("生成邀请二维码失败");
        }
        return Base64.encode(bytes);
    }
}
