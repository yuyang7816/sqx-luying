package com.sqx.app.rental.cart.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁购物车
 */
@Getter
@Setter
@TableName("rent_cart")
@Schema(name = "RentCart", description = "租赁购物车")
public class RentCart implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "购物车ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "规格ID，0表示默认")
    @TableField("spec_id")
    private Long specId;

    @Schema(description = "配件套餐ID，0表示无")
    @TableField("package_id")
    private Long packageId;

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

    @Schema(description = "数量")
    @TableField("qty")
    private Integer qty;

    @Schema(description = "是否勾选：1是 0否")
    @TableField("selected")
    private Integer selected;

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
