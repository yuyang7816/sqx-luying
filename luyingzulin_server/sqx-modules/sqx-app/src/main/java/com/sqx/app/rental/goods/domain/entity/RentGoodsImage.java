package com.sqx.app.rental.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租赁商品图片
 */
@Getter
@Setter
@TableName("rent_goods_image")
@Schema(name = "RentGoodsImage", description = "租赁商品图片")
public class RentGoodsImage implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "图片ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "图片地址")
    @TableField("image_url")
    private String imageUrl;

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
