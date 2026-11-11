package com.sqx.app.rental.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户配送地址
 */
@Data
@Schema(description = "用户配送地址")
@TableName("user_address")
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "地址ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "收货人姓名")
    @TableField("contact_name")
    private String contactName;

    @Schema(description = "收货人手机号")
    @TableField("contact_phone")
    private String contactPhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "详细地址")
    @TableField("detail_address")
    private String detailAddress;

    @Schema(description = "是否默认：1是 0否")
    @TableField("is_default")
    private Integer isDefault;

    @Schema(description = "创建时间")
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
