package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ClaimableCouponVo {

    @Schema(description = "模板ID")
    private Long templateId;

    private String name;
    private String couponType;
    private String couponTypeLabel;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private String conditionText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    @Schema(description = "当前用户是否已领取（含未使用且未过期）")
    private Boolean claimed;
}
