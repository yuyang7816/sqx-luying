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
 * 续租记录
 */
@Getter
@Setter
@TableName("rent_renew_record")
@Schema(name = "RentRenewRecord", description = "续租记录")
public class RentRenewRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("renew_no")
    private String renewNo;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("extra_days")
    private Integer extraDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("old_end_time")
    private LocalDateTime oldEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("new_end_time")
    private LocalDateTime newEndTime;

    @TableField("renew_amount")
    private BigDecimal renewAmount;

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
