package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminUserDepositLogPageBo extends UserDepositLogPageBo {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;
}
