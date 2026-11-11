package com.sqx.app.referral.withdraw.domain.vo;

import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawRecord;
import com.sqx.app.referral.withdraw.domain.entity.WithdrawalAccount;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-05 10:10:32
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WithdrawRecordVo extends WithdrawRecord {

    @Schema(description = "用户信息")
    private UserEntity userEntity;

    @Schema(description = "用户手机号（管理端明文）")
    private String phone;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "提现账户信息")
    private WithdrawalAccount withdrawalAccount;
}
