package com.sqx.app.referral.withdraw.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提现账号
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-06-20 14:02:39
 */
@Getter
@Setter
@TableName("withdrawal_account")
@Schema(name = "WithdrawalAccount", description = "提现账号")
public class WithdrawalAccount implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "id")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户id")
    @TableField("user_id")
    private Long userId;

    @Schema(description = "账户名称")
    @TableField("real_name")
    private String accountName;

    @Schema(description = "账户账号")
    @TableField("account_no")
    private String accountNumber;

    @Schema(description = "账户类型 1支付宝 2微信")
    @TableField("account_type")
    private Integer type;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
