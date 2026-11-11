package com.sqx.app.base.utils;

import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.common.redis.utils.ConfigUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 公众号授权登录配置
 */
public final class OaOAuthConfigUtil {

    private OaOAuthConfigUtil() {
    }

    public static boolean isEnabled() {
        String value = ConfigUtil.getConfigValue(SysConfigConstant.Sys.SYS_OA_OAUTH_ENABLED);
        if (StringUtils.isBlank(value)) {
            return false;
        }
        return StringUtils.equalsAnyIgnoreCase(value.trim(), "1", "true", "yes", "on");
    }
}
