package com.sqx.app.merchant.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 商户核销取货
 */
@Data
@Schema(description = "商户核销取货")
public class MerchantOrderPickupBo {

    @NotBlank(message = "请输入提货码")
    @Schema(description = "提货码（扫码或手动输入）")
    private String pickupCode;
}
