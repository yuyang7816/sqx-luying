package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplateVo {

    private Long id;
    private String templateNo;
    private String name;
    private String couponType;
    private String couponTypeLabel;
    private BigDecimal amount;
    private BigDecimal minAmount;
    private BigDecimal discountRate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    private String scopeType;
    private String scopeValue;
    private Integer status;
    private String statusLabel;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
