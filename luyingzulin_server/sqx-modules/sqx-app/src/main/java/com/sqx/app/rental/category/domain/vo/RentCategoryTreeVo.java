package com.sqx.app.rental.category.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 租赁分类树节点
 */
@Data
@Schema(description = "租赁分类树节点")
public class RentCategoryTreeVo {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "图标地址")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;

    @Schema(description = "子分类")
    private List<RentCategoryTreeVo> children;
}
