package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserCouponPageBo extends PageParam {

    @Schema(description = "状态筛选：unused/used/expired")
    private String status;

    @Schema(description = "券名称关键词")
    private String keyword;
}
