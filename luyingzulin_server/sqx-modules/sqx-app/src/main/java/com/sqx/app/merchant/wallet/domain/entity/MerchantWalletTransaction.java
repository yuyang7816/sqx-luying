package com.sqx.app.merchant.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("merchant_wallet_transaction")
public class MerchantWalletTransaction implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("transaction_type")
    private Integer transactionType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("direction")
    private Integer direction;

    @TableField("biz_type")
    private Integer bizType;

    @TableField("biz_id")
    private Long bizId;

    @TableField("status")
    private Integer status;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("extra_data")
    private String extraData;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
