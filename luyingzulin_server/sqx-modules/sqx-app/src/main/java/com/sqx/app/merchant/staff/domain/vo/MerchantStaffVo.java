package com.sqx.app.merchant.staff.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户员工 VO
 */
@Data
@Schema(description = "商户员工")
public class MerchantStaffVo {

    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "岗位角色")
    private String role;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
