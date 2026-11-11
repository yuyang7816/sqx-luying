package com.sqx.app.base.messages.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "消息未读汇总")
public class MessagesUnreadSummaryVo {

    @Schema(description = "站内系统通知未读数")
    private Integer systemUnread;

    @Schema(description = "IM 未读数")
    private Integer imUnread;

    @Schema(description = "合计未读数")
    private Integer totalUnread;
}
