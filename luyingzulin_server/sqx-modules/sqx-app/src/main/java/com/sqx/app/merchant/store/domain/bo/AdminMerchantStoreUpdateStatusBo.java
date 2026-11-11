package com.sqx.app.merchant.store.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 管理端门店启停入参
 */
@Data
@Schema(description = "管理端门店启停入参")
public class AdminMerchantStoreUpdateStatusBo {

    @Schema(description = "门店ID")
    @NotNull(message = "门店ID不能为空")
    private Long id;

    @Schema(description = "状态：1启用 0停用")
    @NotNull(message = "状态不能为空")
    private Integer status;
}
