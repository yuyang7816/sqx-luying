package com.sqx.app.rental.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁商品库存
 */
@Getter
@Setter
@TableName("rent_stock")
@Schema(name = "RentStock", description = "租赁商品库存")
public class RentStock implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "库存ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "规格ID，0表示默认")
    @TableField("spec_id")
    private Long specId;

    @Schema(description = "总库存")
    @TableField("total_qty")
    private Integer totalQty;

    @Schema(description = "可用库存")
    @TableField("available_qty")
    private Integer availableQty;

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
