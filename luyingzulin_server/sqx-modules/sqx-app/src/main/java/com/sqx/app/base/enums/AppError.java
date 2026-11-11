package com.sqx.app.base.enums;

import com.sqx.common.core.enums.IErrorCode;

/**
 * @author chengming
 * @since 2025/5/14
 *
 * app 异常
 */
public enum AppError implements IErrorCode {
    PHONE_NOT_REGISTER(350001, "手机号未注册"),
    ACCOUNT_ABNORMAL(350002, "账户状态异常"),
    PHONE_OR_PASSWORD_ERROR(350003, "手机号或密码错误"),
    VERIFY_CODE_ERROR(350004, "验证码不正确"),
    PHONE_REGISTERED(350005, "手机号已注册"),
    USER_NOT_FOUND(350006, "用户信息不存在"),
    UNSUPPORTED_CODE(350007, "不支持的业务代码"),
    CONFIG_NOT_FOUND(350007, "系统配置不存在"),
    INFO_NOT_FOUND(350008, "信息不存在"),
    SMS_TEMPLATE_NOT_FOUND(350009, "信息不存在"),
    INVITE_CODE_INVALID(350010, "邀请码无效"),
    OA_OAUTH_DISABLED(350011, "公众号授权登录未开启"),

    ;

    private final Integer code;
    private final String message;

    AppError(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
