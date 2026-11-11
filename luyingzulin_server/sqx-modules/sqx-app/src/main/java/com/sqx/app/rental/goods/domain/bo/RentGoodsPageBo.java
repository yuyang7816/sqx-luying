package com.sqx.app.rental.goods.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租赁商品分页查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "租赁商品分页查询")
public class RentGoodsPageBo extends PageParam {

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "关键词（商品名称/副标题）")
    private String keyword;

    @Schema(description = "租赁类型：day/hour/overnight")
    private String rentType;

    @Schema(description = "排序：default/price/sales")
    private String sortBy;

    @Schema(description = "状态：1上架 0下架，默认查上架")
    private Integer status;

    @Schema(description = "商户ID（商户端筛选用）")
    private Long merchantId;

    @Schema(description = "是否仅展示营业中门店商品（C 端传 true）")
    private Boolean openStoreOnly;

    @Schema(description = "城市（按门店 city 模糊匹配，如 西安 匹配 西安市）")
    private String city;
}
