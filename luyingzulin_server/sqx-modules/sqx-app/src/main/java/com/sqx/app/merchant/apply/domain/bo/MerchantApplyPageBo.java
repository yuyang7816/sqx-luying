package com.sqx.app.merchant.apply.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端入驻申请分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端入驻申请分页入参")
public class MerchantApplyPageBo extends PageParam {

    @Schema(description = "关键词：商家名称/联系人/手机号")
    private String keyword;

    @Schema(description = "状态：pending/reviewing/approved/rejected")
    private String status;
}
