package com.sqx.app.base.platformcs.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "平台客服消息分页")
public class PlatformCsMessagePageBo extends PageParam {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID")
    private Long conversationId;
}
