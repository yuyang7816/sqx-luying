package com.sqx.app.merchant.apply.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端商户分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端商户分页入参")
public class MerchantPageBo extends PageParam {

    @Schema(description = "关键词：商户名称/联系人/手机号/商户编号")
    private String keyword;

    @Schema(description = "状态：active/frozen")
    private String status;
}
