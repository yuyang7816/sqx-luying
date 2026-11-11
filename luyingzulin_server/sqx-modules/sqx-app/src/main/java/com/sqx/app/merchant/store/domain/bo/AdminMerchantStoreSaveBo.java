package com.sqx.app.merchant.store.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 管理端门店保存入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端门店保存入参")
public class AdminMerchantStoreSaveBo extends MerchantStoreSaveBo {

    @Schema(description = "所属商户ID，新增时必填")
    @NotNull(message = "所属商户不能为空")
    private Long merchantId;
}
