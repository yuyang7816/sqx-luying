package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class UserCouponConsumeBo {

    @NotNull(message = "优惠券ID不能为空")
    @Schema(description = "用户优惠券ID")
    private Long couponId;

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "核销订单号")
    private String orderNo;
}
