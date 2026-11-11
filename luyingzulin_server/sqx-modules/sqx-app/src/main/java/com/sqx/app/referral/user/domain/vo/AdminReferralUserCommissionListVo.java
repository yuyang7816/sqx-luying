package com.sqx.app.referral.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "管理端用户佣金列表项")
public class AdminReferralUserCommissionListVo {

    private Long id;

    private Long userId;

    private String userName;

    private String phone;

    private String phoneMasked;

    private BigDecimal commission;

    private Long inviteCount;
}
