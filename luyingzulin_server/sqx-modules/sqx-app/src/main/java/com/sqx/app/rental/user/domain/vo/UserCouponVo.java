package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVo {

    @Schema(description = "用户券ID")
    private Long id;

    @Schema(description = "券编号")
    private String couponNo;

    @Schema(description = "模板编号")
    private String templateNo;

    @Schema(description = "优惠券名称")
    private String name;

    @Schema(description = "类型：amount_off/discount/delivery_free/deposit_free")
    private String couponType;

    @Schema(description = "类型中文")
    private String couponTypeLabel;

    @Schema(description = "面额")
    private BigDecimal amount;

    @Schema(description = "使用门槛")
    private BigDecimal minAmount;

    @Schema(description = "状态：unused/used/expired")
    private String status;

    @Schema(description = "状态中文")
    private String statusLabel;

    @Schema(description = "条件描述")
    private String conditionText;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "使用时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedTime;

    @Schema(description = "核销订单号")
    private String orderNo;
}
