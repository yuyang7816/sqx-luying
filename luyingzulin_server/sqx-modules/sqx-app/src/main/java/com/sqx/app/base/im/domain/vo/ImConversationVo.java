package com.sqx.app.base.im.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "IM 会话")
public class ImConversationVo {

    @Schema(description = "会话ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "对方名称（用户端为商户名，商户端为用户名）")
    private String peerName;

    @Schema(description = "对方头像")
    private String peerAvatar;

    @Schema(description = "最后消息时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMsgTime;

    @Schema(description = "最后消息摘要")
    private String lastMsgPreview;

    @Schema(description = "当前端未读数")
    private Integer unreadCount;
}
