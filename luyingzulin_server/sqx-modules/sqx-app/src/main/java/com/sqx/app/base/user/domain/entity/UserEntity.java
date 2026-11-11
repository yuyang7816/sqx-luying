package com.sqx.app.base.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 用户
 *
 */
@Data
@Schema(description = "用户")
@TableName("tb_user")
public class UserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户id")
    @TableId(type = IdType.AUTO)
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @TableField("user_name")
    private String userName;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

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
    @Schema(description = "用户端小程序openId")
    @TableField("ma_open_id")
    private String maOpenId;

    /**
     * 微信app openid
     */
    @Schema(description = "用户端公众号openId")
    @TableField("mp_open_id")
    private String mpOpenId;

    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 苹果id
     */
    @Schema(description = "苹果id")
    @TableField("apple_id")
    private String appleId;

    /**
     * 手机类型 1安卓 2ios
     */
    @Schema(description = "手机类型 1安卓 2ios")
    @TableField("sys_phone")
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
    private String cid;

    @Schema(description = "用户类型 1普通用户 2vip用户")
    private Integer userType;

    @TableField(exist = false)
    private Integer isVip;

    /**
     * 邀请码
     */
    @Schema(description = "邀请码")
    @TableField("invitation_code")
    private String invitationCode;

    /**
     * 上级邀请码
     */
    @Schema(description = "上级邀请码")
    @TableField("superior_invitation_code")
    private String superiorInvitationCode;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
