package com.sqx.app.rental.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "押金缴纳支付状态")
public class UserDepositPayStatusVo {

    @Schema(description = "押金支付单号 DP 前缀")
    private String payNo;

    @Schema(description = "pending/completed")
    private String status;

    @Schema(description = "是否已支付成功")
    private Boolean paid;
}
