package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CouponClaimBo {

    @NotNull(message = "优惠券模板ID不能为空")
    @Schema(description = "优惠券模板ID")
    private Long templateId;
}
