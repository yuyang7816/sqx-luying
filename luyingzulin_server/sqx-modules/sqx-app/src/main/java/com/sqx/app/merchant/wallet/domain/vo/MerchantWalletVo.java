package com.sqx.app.merchant.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商户经营钱包概览")
public class MerchantWalletVo {

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "可用余额")
    private BigDecimal amount;

    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;
}
