package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "管理端-用户押金退还申请")
public class AdminUserDepositRefundVo {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "流水号")
    private String logNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "手机号（脱敏）")
    private String phoneMasked;

    @Schema(description = "联系电话")
    private String phone;

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
