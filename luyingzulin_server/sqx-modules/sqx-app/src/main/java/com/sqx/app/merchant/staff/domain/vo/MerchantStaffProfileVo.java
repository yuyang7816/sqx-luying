package com.sqx.app.merchant.staff.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 当前登录用户的员工身份
 */
@Data
@Schema(description = "员工身份")
public class MerchantStaffProfileVo {

    @Schema(description = "是否为启用中的核销员工")
    private Boolean active;

    @Schema(description = "员工ID")
    private Long staffId;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "岗位")
    private String role;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "门店名称")
    private String storeName;
}
