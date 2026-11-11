package com.sqx.app.merchant.order.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户订单（用户租赁订单同步）
 */
@Getter
@Setter
@TableName("merchant_order")
public class MerchantOrder implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_order_no")
    private String merchantOrderNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("store_id")
    private Long storeId;

    @TableField("rent_order_id")
    private Long rentOrderId;

    @TableField("rent_order_no")
    private String rentOrderNo;

    @TableField("user_id")
    private Long userId;

    @TableField("goods_name")
    private String goodsName;

    /** pending_prepare / prepared / renting / returning / inspecting / completed */
    @TableField("status")
    private String status;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("pickup_type")
    private String pickupType;

    @TableField("address")
    private String address;

    @TableField("pay_amount")
    private BigDecimal payAmount;

    @TableField("remark")
    private String remark;

    @TableField("prepare_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime prepareTime;

    @TableField("pickup_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;

    @TableField("complete_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 生成 LINK 订单号 */
    public static String buildLinkNo(Long rentOrderId) {
        return "LINK-" + rentOrderId;
    }
}
