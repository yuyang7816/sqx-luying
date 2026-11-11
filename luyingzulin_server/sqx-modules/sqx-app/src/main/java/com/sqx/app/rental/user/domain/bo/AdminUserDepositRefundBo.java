package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AdminUserDepositRefundBo {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "退押金额不能为空")
    @DecimalMin(value = "0.01", message = "退押金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "运营备注")
    private String remark;
}
