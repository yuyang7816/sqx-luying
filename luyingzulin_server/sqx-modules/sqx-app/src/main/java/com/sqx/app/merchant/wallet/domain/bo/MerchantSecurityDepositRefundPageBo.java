package com.sqx.app.merchant.wallet.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端-商户保证金退还申请分页")
public class MerchantSecurityDepositRefundPageBo extends PageParam {

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "状态 pending/completed/rejected")
    private String status;
}
