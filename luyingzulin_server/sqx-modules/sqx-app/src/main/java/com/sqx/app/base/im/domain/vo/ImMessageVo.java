package com.sqx.app.base.im.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "IM 消息")
public class ImMessageVo {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "会话ID")
    private Long conversationId;

    @Schema(description = "发送方：user/merchant/system")
    private String senderRole;

    @Schema(description = "消息类型：text/video/image")
    private String msgType;

    @Schema(description = "文本内容")
    private String content;

    @Schema(description = "媒体URL")
    private String mediaUrl;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "关联商品ID")
    private Long goodsId;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "发送时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
