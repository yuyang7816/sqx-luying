package com.sqx.app.rental.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁取货码
 */
@Getter
@Setter
@TableName("rent_pickup_code")
@Schema(name = "RentPickupCode", description = "租赁取货码")
public class RentPickupCode implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("pickup_code")
    private String pickupCode;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("used")
    private Integer used;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("use_time")
    private LocalDateTime useTime;

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
