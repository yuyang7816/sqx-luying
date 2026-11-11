package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "管理端-用户押金退还审核")
public class AdminUserDepositRefundAuditBo {

    @NotNull(message = "流水ID不能为空")
    @Schema(description = "押金流水ID")
    private Long logId;

    @NotBlank(message = "审核动作不能为空")
    @Schema(description = "approve/reject")
    private String action;

    @Schema(description = "审核备注")
    private String remark;
}
