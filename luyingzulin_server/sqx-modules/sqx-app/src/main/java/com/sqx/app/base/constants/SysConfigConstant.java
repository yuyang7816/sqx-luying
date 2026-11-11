package com.sqx.app.base.constants;

/**
 * @author chengming
 * @since 2025/12/19
 */
public interface SysConfigConstant {

    /**
     * 系统配置
     */
    interface Sys {
        /**
         * 用户端小程序appid
         */
        String SYS_USER_MA_APPID = "sys_user_ma_appid";

        /**
         * 用户端小程序secret
         */
        String SYS_USER_MA_SECRET = "sys_user_ma_secret";

        /**
         * 用户端公众号 appid（H5 微信内置浏览器 OAuth）
         */
        String SYS_USER_MP_APPID = "sys_user_mp_appid";

        /**
         * 用户端公众号 secret
         */
        String SYS_USER_MP_SECRET = "sys_user_mp_secret";

        /**
         * h5服务域名配置
         */
        String SYS_H5_DOMAIN = "sys_h5_domain";

        /**
         * 公众号授权登录开关 1=开 0=关
         */
        String SYS_OA_OAUTH_ENABLED = "sys_oa_oauth_enabled";

        /**
         * 用户端小程序订单订阅消息模板 ID（多个英文逗号分隔）
         */
        String SYS_USER_MA_SUBSCRIBE_ORDER_NOTIFY = "sys_user_ma_subscribe_order_notify";
    }
}
