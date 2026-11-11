package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CouponTemplatePageBo extends PageParam {

    @Schema(description = "名称关键词")
    private String keyword;

    @Schema(description = "类型")
    private String couponType;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
