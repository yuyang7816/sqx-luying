package com.sqx.app.rental.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户商品收藏
 */
@Data
@Schema(description = "用户商品收藏")
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收藏ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "商品ID")
    @TableField("goods_id")
    private Long goodsId;

    @Schema(description = "收藏时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
