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
 * 租赁订单
 */
@Getter
@Setter
@TableName("rent_order")
@Schema(name = "RentOrder", description = "租赁订单")
public class RentOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "订单号")
    @TableField("order_no")
    private String orderNo;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商户ID")
    @TableField("merchant_id")
    private Long merchantId;

    @Schema(description = "门店ID")
    @TableField("store_id")
    private Long storeId;

    @Schema(description = "订单状态")
    @TableField("status")
    private String status;

    @Schema(description = "租赁类型：day/hour/overnight")
    @TableField("rent_type")
    private String rentType;

    @Schema(description = "租期开始")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "租期结束")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    private LocalDateTime endTime;

    @Schema(description = "租赁天数")
    @TableField("days")
    private Integer days;

    @Schema(description = "取货方式：store/pickup_point/delivery")
    @TableField("pickup_type")
    private String pickupType;

    @Schema(description = "自提门店ID")
    @TableField("pickup_store_id")
    private Long pickupStoreId;

    @Schema(description = "配送地址ID")
    @TableField("address_id")
    private Long addressId;

    @Schema(description = "联系人")
    @TableField("contact_name")
    private String contactName;

    @Schema(description = "联系电话")
    @TableField("contact_phone")
    private String contactPhone;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "租金合计")
    @TableField("rent_amount")
    private BigDecimal rentAmount;

    @Schema(description = "押金合计")
    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @Schema(description = "配送费")
    @TableField("delivery_amount")
    private BigDecimal deliveryAmount;

    @Schema(description = "优惠金额")
    @TableField("coupon_amount")
    private BigDecimal couponAmount;

    @Schema(description = "使用的用户优惠券ID")
    @TableField("coupon_id")
    private Long couponId;

    @Schema(description = "应付总额")
    @TableField("pay_amount")
    private BigDecimal payAmount;

    @Schema(description = "取货码")
    @TableField("pickup_code")
    private String pickupCode;

    @Schema(description = "支付时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("pay_time")
    private LocalDateTime payTime;

    @Schema(description = "取货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("pickup_time")
    private LocalDateTime pickupTime;

    @Schema(description = "归还时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("return_time")
    private LocalDateTime returnTime;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("complete_time")
    private LocalDateTime completeTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
