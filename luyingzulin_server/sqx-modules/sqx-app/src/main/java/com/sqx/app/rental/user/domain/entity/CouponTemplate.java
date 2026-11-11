package com.sqx.app.rental.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 */
@Getter
@Setter
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("template_no")
    private String templateNo;

    @TableField("name")
    private String name;

    /** 类型：amount_off/discount/delivery_free/deposit_free */
    @TableField("coupon_type")
    private String couponType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("min_amount")
    private BigDecimal minAmount;

    @TableField("discount_rate")
    private BigDecimal discountRate;

    @TableField("valid_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStart;

    @TableField("valid_end")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEnd;

    @TableField("scope_type")
    private String scopeType;

    @TableField("scope_value")
    private String scopeValue;

    /** 1启用 0停用 */
    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;

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
