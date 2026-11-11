package com.sqx.app.merchant.wallet.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理端-商户保证金退还申请")
public class AdminMerchantSecurityDepositRefundVo {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "流水号")
    private String logNo;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "退还金额")
    private BigDecimal amount;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "申请备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime createTime;
}
