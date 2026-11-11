package com.sqx.app.merchant.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "商户保证金支付状态")
public class MerchantSecurityDepositPayStatusVo {

    private String payNo;
    private String status;
    private Boolean paid;
}
