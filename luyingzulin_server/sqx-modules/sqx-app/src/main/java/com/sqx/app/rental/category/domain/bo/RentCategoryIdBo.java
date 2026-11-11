package com.sqx.app.rental.category.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 租赁分类ID入参
 */
@Data
@Schema(description = "租赁分类ID入参")
public class RentCategoryIdBo {

    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long id;
}
