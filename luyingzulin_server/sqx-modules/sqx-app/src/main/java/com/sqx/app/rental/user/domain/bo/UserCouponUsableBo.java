package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class UserCouponUsableBo {

    @Schema(description = "租金合计（不含押金）")
    private BigDecimal rentAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryAmount;

    @Schema(description = "押金合计")
    private BigDecimal depositAmount;
}
