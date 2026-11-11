package com.sqx.app.merchant.wallet.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "管理端-商户保证金退还审核")
public class MerchantSecurityDepositRefundAuditBo {

    @NotNull(message = "流水ID不能为空")
    @Schema(description = "保证金流水ID")
    private Long logId;

    @NotBlank(message = "审核动作不能为空")
    @Schema(description = "approve/reject")
    private String action;

    @Schema(description = "审核备注")
    private String remark;
}
