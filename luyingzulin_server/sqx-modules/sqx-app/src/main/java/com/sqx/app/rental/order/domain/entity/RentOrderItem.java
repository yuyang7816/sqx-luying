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
 * 租赁订单明细
 */
@Getter
@Setter
@TableName("rent_order_item")
@Schema(name = "RentOrderItem", description = "租赁订单明细")
public class RentOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private Long orderId;

    @TableField("order_no")
    private String orderNo;

    @TableField("goods_id")
    private Long goodsId;

    @TableField("goods_name")
    private String goodsName;

    @TableField("spec_info")
    private String specInfo;

    @TableField("package_id")
    private Long packageId;

    @TableField("package_name")
    private String packageName;

    @TableField("package_desc")
    private String packageDesc;

    @TableField("cover_image")
    private String coverImage;

    @TableField("rent_type")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("days")
    private Integer days;

    @TableField("qty")
    private Integer qty;

    @TableField("rent_price")
    private BigDecimal rentPrice;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("item_amount")
    private BigDecimal itemAmount;

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
