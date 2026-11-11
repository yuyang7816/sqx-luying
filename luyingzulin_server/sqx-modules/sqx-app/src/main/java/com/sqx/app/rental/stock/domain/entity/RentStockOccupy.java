package com.sqx.app.rental.stock.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁时段库存占用
 */
@Getter
@Setter
@TableName("rent_stock_occupy")
@Schema(name = "RentStockOccupy", description = "租赁时段库存占用")
public class RentStockOccupy implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "占用ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "规格ID，0表示默认")
    @TableField("spec_id")
    private Long specId;

    @Schema(description = "订单ID")
    @TableField("order_id")
    private Long orderId;

    @Schema(description = "订单号")
    @TableField("order_no")
    private String orderNo;

    @Schema(description = "占用开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    private LocalDateTime startTime;

    @Schema(description = "占用结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    private LocalDateTime endTime;

    @Schema(description = "占用数量")
    @TableField("qty")
    private Integer qty;

    @Schema(description = "状态：PRELOCK/OCCUPIED/RELEASED")
    @TableField("status")
    private String status;

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
