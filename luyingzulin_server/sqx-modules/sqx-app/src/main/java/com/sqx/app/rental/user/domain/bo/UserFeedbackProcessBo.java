package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserFeedbackProcessBo {

    @NotNull(message = "反馈ID不能为空")
    private Long id;

    @NotBlank(message = "处理备注不能为空")
    @Schema(description = "处理备注")
    private String replyRemark;
}
