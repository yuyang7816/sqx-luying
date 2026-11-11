package com.sqx.app.merchant.apply.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端商户列表项
 */
@Data
@Schema(description = "管理端商户列表项")
public class MerchantListVo {

    @Schema(description = "商户ID")
    private Long id;

    @Schema(description = "商户编号")
    private String merchantNo;

    @Schema(description = "商户名称")
    private String name;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "负责人用户ID")
    private Long userId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "商户保证金余额")
    private BigDecimal securityDepositAmount;

    @Schema(description = "应缴保证金")
    private BigDecimal securityDepositRequiredAmount;

    @Schema(description = "保证金是否已缴足")
    private Boolean securityDepositPaid;

    @Schema(description = "保证金状态文案")
    private String securityDepositStatusLabel;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
