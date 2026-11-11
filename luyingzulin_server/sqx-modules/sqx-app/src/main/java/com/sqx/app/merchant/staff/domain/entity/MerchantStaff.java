package com.sqx.app.merchant.staff.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商户门店员工
 */
@Getter
@Setter
@TableName("merchant_staff")
public class MerchantStaff implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("store_id")
    private Long storeId;

    @TableField("user_id")
    private Long userId;

    @TableField("name")
    private String name;

    @TableField("role")
    private String role;

    @TableField("phone")
    private String phone;

    /** 1启用 0停用 */
    @TableField("status")
    private Integer status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
