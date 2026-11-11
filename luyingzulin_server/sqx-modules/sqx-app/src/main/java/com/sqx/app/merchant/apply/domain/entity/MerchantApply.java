package com.sqx.app.merchant.apply.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户入驻申请
 */
@Getter
@Setter
@TableName("merchant_apply")
public class MerchantApply implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("apply_no")
    private String applyNo;

    @TableField("user_id")
    private Long userId;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("store_address")
    private String storeAddress;

    @TableField("store_name")
    private String storeName;

    @TableField("business_hours")
    private String businessHours;

    @TableField("service_area")
    private String serviceArea;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("base_delivery_fee")
    private BigDecimal baseDeliveryFee;

    @TableField("free_delivery_threshold")
    private BigDecimal freeDeliveryThreshold;

    @TableField("business_scope")
    private String businessScope;

    @TableField("license_urls")
    private String licenseUrls;

    @TableField("extra_urls")
    private String extraUrls;

    /** pending / reviewing / approved / rejected */
    @TableField("status")
    private String status;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("audit_admin_id")
    private Long auditAdminId;

    @TableField("audit_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("store_id")
    private Long storeId;

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
