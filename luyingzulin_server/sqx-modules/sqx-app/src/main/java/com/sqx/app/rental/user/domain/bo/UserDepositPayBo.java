package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class UserDepositPayBo {

    @NotNull(message = "缴纳金额不能为空")
    @DecimalMin(value = "0.01", message = "缴纳金额必须大于0")
    @Schema(description = "缴纳金额（元）")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;
}
