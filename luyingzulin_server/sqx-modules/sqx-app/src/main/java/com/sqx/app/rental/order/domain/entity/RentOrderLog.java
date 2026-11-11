package com.sqx.app.rental.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁订单状态日志
 */
@Getter
@Setter
@TableName("rent_order_log")
@Schema(name = "RentOrderLog", description = "租赁订单状态日志")
public class RentOrderLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("operator_type")
    private String operatorType;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("remark")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
