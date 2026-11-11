package com.sqx.app.rental.category.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 租赁分类保存入参
 */
@Data
@Schema(description = "租赁分类保存入参")
public class RentCategorySaveBo {

    @Schema(description = "分类ID（编辑时必填）")
    private Long id;

    @Schema(description = "父分类ID，根为0")
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标地址")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;
}
