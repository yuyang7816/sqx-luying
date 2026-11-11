package com.sqx.app.merchant.apply.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理端入驻审核入参
 */
@Data
@Schema(description = "管理端入驻审核入参")
public class MerchantApplyAuditBo {

    @Schema(description = "申请ID")
    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    @Schema(description = "审核结果：approved/rejected")
    @NotBlank(message = "审核结果不能为空")
    private String auditResult;

    @Schema(description = "驳回原因（驳回时必填）")
    private String rejectReason;
}
