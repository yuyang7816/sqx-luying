package com.sqx.app.base.platformcs.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "平台客服会话")
public class PlatformCsConversationVo {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "对方展示名（用户端为官方客服）")
    private String peerName;

    @Schema(description = "对方头像（用户端为官方客服头像）")
    private String peerAvatar;

    @Schema(description = "最后消息时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMsgTime;

    @Schema(description = "最后消息摘要")
    private String lastMsgPreview;

    @Schema(description = "当前端未读数")
    private Integer unreadCount;

    @Schema(description = "平台客服未读数")
    private Integer adminUnread;
}
