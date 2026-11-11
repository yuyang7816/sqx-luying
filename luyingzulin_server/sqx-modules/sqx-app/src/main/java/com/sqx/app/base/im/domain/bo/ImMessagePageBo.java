package com.sqx.app.base.im.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "IM 消息分页")
public class ImMessagePageBo extends PageParam {

    @Schema(description = "会话ID")
    @NotNull(message = "会话ID不能为空")
    private Long conversationId;
}
