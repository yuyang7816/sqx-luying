package com.sqx.app.base.im.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "发送 IM 文本消息")
public class ImSendTextBo {

    @Schema(description = "会话ID")
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;

    @Schema(description = "文本内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
