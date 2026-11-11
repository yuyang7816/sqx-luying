package com.sqx.app.wallet.wallet.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class UserWalletRechargeBo {

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    @Schema(description = "充值金额（元）")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;
}
