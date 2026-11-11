package com.sqx.app.rental.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户优惠券
 */
@Getter
@Setter
@TableName("user_coupon")
public class UserCoupon implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("coupon_no")
    private String couponNo;

    @TableField("user_id")
    private Long userId;

    @TableField("template_id")
    private Long templateId;

    /** unused/used/expired */
    @TableField("status")
    private String status;

    @TableField("order_no")
    private String orderNo;

    @TableField("used_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime usedTime;

    @TableField("expire_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
