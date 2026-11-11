package com.sqx.app.merchant.apply.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 管理端商户状态变更入参
 */
@Data
@Schema(description = "管理端商户状态变更入参")
public class MerchantUpdateStatusBo {

    @Schema(description = "商户ID")
    @NotNull(message = "商户ID不能为空")
    private Long id;

    @Schema(description = "状态：active启用 / frozen冻结")
    @NotBlank(message = "状态不能为空")
    private String status;
}
