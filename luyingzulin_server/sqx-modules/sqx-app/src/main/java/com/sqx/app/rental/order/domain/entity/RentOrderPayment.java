package com.sqx.app.rental.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租赁订单支付记录
 */
@Getter
@Setter
@TableName("rent_order_payment")
@Schema(name = "RentOrderPayment", description = "租赁订单支付记录")
public class RentOrderPayment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("pay_no")
    private String payNo;

    @TableField("pay_biz_type")
    private String payBizType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("pay_status")
    private String payStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
