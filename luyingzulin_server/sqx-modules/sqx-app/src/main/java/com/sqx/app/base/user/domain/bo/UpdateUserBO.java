package com.sqx.app.base.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author chengming
 * @since 2025/4/24
 */
@Data
public class UpdateUserBO {

    /**
     * 用户ID
     */
    @Schema(description = "用户id")
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String userName;

    /**
     * 头像
     */
    @Schema(description = "头像")
    private String avatar;

    /**
     * 性别 1男 2女
     */
    @Schema(description = "性别 1男 2女")
    private Integer sex;

    /**
     * 年龄
     */
    @Schema(description = "年龄")
    private Integer age;

    /**
     * 微信小程序openid
     */
    @Schema(description = "微信小程序openid")

    private String openId;

    /**
     * 微信app openid
     */
    @Schema(description = "微信公众号openid")
    private String wxOpenId;

    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 苹果id
     */
    @Schema(description = "苹果id")
    private String appleId;

    /**
     * 手机类型 1安卓 2ios
     */
    @Schema(description = "手机类型 1安卓 2ios")
    private Integer sysPhone;

    /**
     * 状态 1正常 2禁用
     */
    @Schema(description = "状态 1正常 2禁用")
    private Integer status;

    /**
     * 来源 app 小程序 公众号
     */
    @Schema(description = "来源 app 小程序 公众号")
    private String platform;

    @Schema(description = "app推送id")
    private String clientId;

    /**
     * 邀请码
     */
    @Schema(description = "邀请码")
    private String invitationCode;

    @Schema(description = "手机号（管理端不可修改，仅用于校验）")
    private String phone;

    @Schema(description = "是否是会员(0不是 1是)")
    private Integer isVip;

    @Schema(description = "用户类型 1普通用户 2vip用户")
    private Integer userType;
}
