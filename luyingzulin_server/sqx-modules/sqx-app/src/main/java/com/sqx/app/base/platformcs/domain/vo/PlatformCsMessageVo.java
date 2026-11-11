package com.sqx.app.base.platformcs.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "平台客服消息")
public class PlatformCsMessageVo {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送方角色 user/admin")
    private String senderRole;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
