package com.sqx.app.referral.withdraw.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-05 10:10:32
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WithdrawRecordPageBo extends PageParam {

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "提现来源 1佣金 2用户钱包 3商户钱包")
    private Integer sourceType;
}
