package com.sqx.app.merchant.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商户经营钱包（与用户个人钱包 user_wallet 分离）
 */
@Getter
@Setter
@TableName("merchant_wallet")
public class MerchantWallet implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("merchant_id")
    private Long merchantId;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("frozen_amount")
    private BigDecimal frozenAmount;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
