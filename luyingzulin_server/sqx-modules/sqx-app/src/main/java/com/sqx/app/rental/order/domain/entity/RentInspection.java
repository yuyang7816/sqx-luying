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
 * 验收记录
 */
@Getter
@Setter
@TableName("rent_inspection")
@Schema(name = "RentInspection", description = "验收记录")
public class RentInspection implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("inspector_type")
    private String inspectorType;

    @TableField("inspector_id")
    private Long inspectorId;

    @TableField("device_condition")
    private String deviceCondition;

    @TableField("deduct_amount")
    private BigDecimal deductAmount;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("remark")
    private String remark;

    @TableField("audit_status")
    private String auditStatus;

    @TableField("audit_remark")
    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("audit_time")
    private LocalDateTime auditTime;

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
