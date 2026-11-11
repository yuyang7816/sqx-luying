package com.sqx.app.base.platformcs.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "平台客服会话分页")
public class PlatformCsConversationPageBo extends PageParam {

    @Schema(description = "关键词（用户昵称/手机号）")
    private String keyword;

    @Schema(description = "仅未读")
    private Boolean unreadOnly;
}
