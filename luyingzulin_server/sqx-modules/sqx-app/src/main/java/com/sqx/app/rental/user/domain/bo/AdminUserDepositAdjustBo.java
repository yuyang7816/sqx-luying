package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Schema(description = "管理端押金调整")
public class AdminUserDepositAdjustBo {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "0.01", message = "调整金额必须大于0")
    private BigDecimal amount;

    /** 1=增加 2=减少 */
    @NotNull(message = "调整方向不能为空")
    private Integer direction;

    private String remark;
}
