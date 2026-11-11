package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AdminUserDepositPageBo extends PageParam {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户手机号")
    private String phone;

    @Schema(description = "状态：1正常 0冻结")
    private Integer status;
}
