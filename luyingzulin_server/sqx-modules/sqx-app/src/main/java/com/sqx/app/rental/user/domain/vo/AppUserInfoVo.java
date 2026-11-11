package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端个人资料 VO（不含密码等敏感字段）
 */
@Data
@Schema(description = "用户端个人资料")
public class AppUserInfoVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名/昵称")
    private String userName;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别 1男 2女")
    private Integer sex;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "手机号（脱敏，C端展示）")
    private String phoneMasked;

    @Schema(description = "手机号（管理端明文）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phone;

    @Schema(description = "上级邀请码（管理端）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String superiorInvitationCode;

    @Schema(description = "状态 1正常 2禁用（管理端）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer status;

    @Schema(description = "用户类型 1普通用户 2vip用户")
    private Integer userType;

    @Schema(description = "用户类型文案")
    private String userTypeLabel;

    @Schema(description = "是否是会员(0不是 1是)")
    private Integer isVip;

    @Schema(description = "邀请码")
    private String invitationCode;

    @Schema(description = "注册来源 app/小程序/公众号")
    private String platform;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "地址数量（管理端统计，C端资料接口不返回）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer addressCount;

    @Schema(description = "收藏数量（管理端统计，C端资料接口不返回）")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer favoriteCount;
}
