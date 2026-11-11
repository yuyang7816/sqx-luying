package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
public class UserFeedbackSubmitBo {

    @NotBlank(message = "反馈类型不能为空")
    @Schema(description = "类型：order/order_service/product/suggestion/other")
    private String feedbackType;

    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "图片URL列表（上传组件）")
    private List<String> imageUrls;
}
