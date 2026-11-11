package com.sqx.app.wallet.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserWalletRechargePayStatusVo {

    @Schema(description = "充值支付单号")
    private String payNo;

    @Schema(description = "状态：pending / completed")
    private String status;

    @Schema(description = "是否已到账")
    private Boolean paid;
}
