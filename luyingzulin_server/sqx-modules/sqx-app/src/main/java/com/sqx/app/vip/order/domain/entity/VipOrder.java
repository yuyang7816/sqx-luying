package com.sqx.app.vip.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("vip_order")
@Schema(name = "VipOrder", description = "会员购买订单")
public class VipOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("plan_id")
    private Long planId;

    @TableField("plan_name")
    private String planName;

    @TableField("month")
    private Integer month;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("status")
    private String status;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("pay_trade_no")
    private String payTradeNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
