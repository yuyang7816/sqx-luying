package com.sqx.app.vip.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema(description = "购买会员套餐")
public class VipPurchaseBo {

    @NotNull(message = "套餐ID不能为空")
    @Schema(description = "会员套餐ID")
    private Long planId;
}
