package com.sqx.app.rental.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租赁商品配件套餐
 */
@Getter
@Setter
@TableName("rent_goods_package")
@Schema(name = "RentGoodsPackage", description = "租赁商品配件套餐")
public class RentGoodsPackage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "套餐ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "套餐名称")
    @TableField("package_name")
    private String packageName;

    @Schema(description = "套餐说明")
    @TableField("package_desc")
    private String packageDesc;

    @Schema(description = "套餐加价（元）")
    @TableField("price")
    private BigDecimal price;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

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
