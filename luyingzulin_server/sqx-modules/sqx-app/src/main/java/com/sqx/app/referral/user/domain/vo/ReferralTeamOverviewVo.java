package com.sqx.app.referral.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReferralTeamOverviewVo {

    @Schema(description = "一级邀请人数")
    private Long level1Count;

    @Schema(description = "二级邀请人数")
    private Long level2Count;

    @Schema(description = "累计收益")
    private BigDecimal totalCommission;

    @Schema(description = "一级收益")
    private BigDecimal level1Commission;

    @Schema(description = "二级收益")
    private BigDecimal level2Commission;

    @Schema(description = "一级返佣比例（百分比）")
    private BigDecimal level1RatePercent;

    @Schema(description = "二级返佣比例（百分比）")
    private BigDecimal level2RatePercent;
}
