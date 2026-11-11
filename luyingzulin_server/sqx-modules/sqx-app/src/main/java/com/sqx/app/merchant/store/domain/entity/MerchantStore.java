package com.sqx.app.merchant.store.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户门店
 */
@Getter
@Setter
@TableName("merchant_store")
public class MerchantStore implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("store_no")
    private String storeNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("name")
    private String name;

    @TableField("address")
    private String address;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("district")
    private String district;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("business_hours")
    private String businessHours;

    @TableField("service_area")
    private String serviceArea;

    @TableField("longitude")
    private BigDecimal longitude;

    @TableField("latitude")
    private BigDecimal latitude;

    @TableField("delivery_enabled")
    private Integer deliveryEnabled;

    @TableField("self_pickup_enabled")
    private Integer selfPickupEnabled;

    @TableField("base_delivery_fee")
    private BigDecimal baseDeliveryFee;

    @TableField("free_delivery_threshold")
    private BigDecimal freeDeliveryThreshold;

    /** 1启用 0停用 */
    @TableField("status")
    private Integer status;

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
