package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserFeedbackPageBo extends PageParam {

    @Schema(description = "反馈类型")
    private String feedbackType;

    @Schema(description = "状态：pending/processed")
    private String status;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "关键词（内容/编号）")
    private String keyword;
}
