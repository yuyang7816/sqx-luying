package com.sqx.app.wallet.wallet.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UserWalletPayBalanceBo {

    @NotBlank(message = "订单号不能为空")
    @Schema(description = "业务订单号")
    private String orderNo;

    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "业务类型")
    private String businessType;
}
