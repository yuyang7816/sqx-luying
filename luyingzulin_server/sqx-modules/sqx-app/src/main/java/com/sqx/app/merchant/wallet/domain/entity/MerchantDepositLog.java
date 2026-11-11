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
@TableName("merchant_deposit_log")
public class MerchantDepositLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("log_no")
    private String logNo;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("log_type")
    private String logType;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("order_no")
    private String orderNo;

    @TableField("status")
    private String status;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
