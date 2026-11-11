package com.sqx.app.merchant.goods.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户商品分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商户商品分页入参")
public class MerchantGoodsPageBo extends PageParam {

    @Schema(description = "关键词：商品名称/编号")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "状态：1上架 0下架")
    private Integer status;
}
