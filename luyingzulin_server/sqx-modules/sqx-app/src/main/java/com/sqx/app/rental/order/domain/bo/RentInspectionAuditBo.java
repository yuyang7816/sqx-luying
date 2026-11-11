package com.sqx.app.rental.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理端-验收复核
 */
@Data
@Schema(description = "验收复核")
public class RentInspectionAuditBo {

    @NotNull(message = "验收记录ID不能为空")
    @Schema(description = "验收记录ID")
    private Long id;

    @NotBlank(message = "复核结果不能为空")
    @Schema(description = "复核结果：approved/rejected")
    private String auditStatus;

    @Schema(description = "复核备注")
    private String auditRemark;
}
