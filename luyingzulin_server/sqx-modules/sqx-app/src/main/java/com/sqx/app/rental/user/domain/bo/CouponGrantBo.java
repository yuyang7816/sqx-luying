package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CouponGrantBo {

    @NotNull(message = "模板ID不能为空")
    @Schema(description = "优惠券模板ID")
    private Long templateId;

    @Schema(description = "指定用户ID列表（为空则忽略）")
    private List<Long> userIds;

    @Schema(description = "是否按手机号发放")
    private List<String> phones;
}
