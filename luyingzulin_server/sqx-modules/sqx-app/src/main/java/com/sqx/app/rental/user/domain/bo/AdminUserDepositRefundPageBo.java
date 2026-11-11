package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端-用户押金退还申请分页")
public class AdminUserDepositRefundPageBo extends PageParam {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "状态 pending/completed/rejected")
    private String status;
}
