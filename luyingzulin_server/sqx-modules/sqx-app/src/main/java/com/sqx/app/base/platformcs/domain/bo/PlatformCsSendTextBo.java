package com.sqx.app.base.platformcs.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "平台客服发送文本")
public class PlatformCsSendTextBo {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "文本内容")
    private String content;
}
