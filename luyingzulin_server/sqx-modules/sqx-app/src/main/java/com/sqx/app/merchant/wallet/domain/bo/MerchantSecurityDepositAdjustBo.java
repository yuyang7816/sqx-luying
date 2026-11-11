package com.sqx.app.merchant.wallet.domain.bo;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class MerchantSecurityDepositAdjustBo {

    @NotNull(message = "商户ID不能为空")
    private Long merchantId;

    @NotNull(message = "调整金额不能为空")
    @DecimalMin(value = "0.01", message = "调整金额必须大于0")
    private BigDecimal amount;

    /** add=增加 reduce=扣减 */
    @NotBlank(message = "调整方向不能为空")
    private String direction;

    private String remark;
}
