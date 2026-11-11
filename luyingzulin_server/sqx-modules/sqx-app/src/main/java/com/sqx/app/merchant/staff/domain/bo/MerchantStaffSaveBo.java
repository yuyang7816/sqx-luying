package com.sqx.app.merchant.staff.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 商户员工保存入参
 */
@Data
@Schema(description = "商户员工保存入参")
public class MerchantStaffSaveBo {

    @Schema(description = "员工ID，编辑时必填")
    private Long id;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "员工姓名")
    @NotBlank(message = "员工姓名不能为空")
    private String name;

    @Schema(description = "岗位角色")
    @NotBlank(message = "岗位角色不能为空")
    private String role;

    @Schema(description = "手机号")
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @Schema(description = "状态：1启用 0停用")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
