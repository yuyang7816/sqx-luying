package com.sqx.app.base.platformcs.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "平台客服未读汇总")
public class PlatformCsUnreadSummaryVo {

    @Schema(description = "平台客服未读会话数")
    private Integer unreadConversations;

    @Schema(description = "平台客服未读消息总数")
    private Integer unreadMessages;
}
