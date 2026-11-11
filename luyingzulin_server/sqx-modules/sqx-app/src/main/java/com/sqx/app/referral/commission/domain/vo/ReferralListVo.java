package com.sqx.app.referral.commission.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReferralListVo {

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
