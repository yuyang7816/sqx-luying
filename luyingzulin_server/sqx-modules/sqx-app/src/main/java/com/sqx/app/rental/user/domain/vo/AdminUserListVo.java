package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端 C 端用户列表项（不含密码）
 */
@Data
@Schema(description = "管理端C端用户列表项")
public class AdminUserListVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "手机号（脱敏，兼容旧字段）")
    private String phoneMasked;

    @Schema(description = "手机号（管理端明文）")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "状态 1正常 2禁用")
    private Integer status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "用户类型 1普通用户 2vip用户")
    private Integer userType;

    @Schema(description = "用户类型文案")
    private String userTypeLabel;

    @Schema(description = "是否是会员(0不是 1是)")
    private Integer isVip;

    @Schema(description = "注册来源")
    private String platform;

    @Schema(description = "邀请码")
    private String invitationCode;

    @Schema(description = "邀请人用户ID")
    private Long inviterUserId;

    @Schema(description = "邀请人用户名")
    private String inviterUserName;

    @Schema(description = "邀请人手机号")
    private String inviterPhone;

    @Schema(description = "直接邀请人数")
    private Long inviteCount;

    @Schema(description = "被邀请人摘要（最多3人）")
    private String inviteeSummary;

    @Schema(description = "分销佣金余额")
    private BigDecimal commission;

    @Schema(description = "分销佣金账户ID")
    private Long commissionId;

    @Schema(description = "注册时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
