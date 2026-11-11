package com.sqx.app.merchant.store.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端门店分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端门店分页入参")
public class AdminMerchantStorePageBo extends PageParam {

    @Schema(description = "关键词：门店名称/地址/联系电话")
    private String keyword;

    @Schema(description = "所属商户ID")
    private Long merchantId;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
