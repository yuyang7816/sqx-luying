package com.sqx.app.base.auth.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 邀请码查询结果
 */
@Data
public class InviteCodeInfoVo {

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "邀请人用户ID")
    private Long inviterId;

    @Schema(description = "邀请人昵称")
    private String inviterName;

    @Schema(description = "邀请人手机号（脱敏）")
    private String inviterMobile;
}
