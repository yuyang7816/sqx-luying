package com.sqx.app.rental.goods.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 租赁商品新增入参
 */
@Data
@Schema(description = "租赁商品新增入参")
public class RentGoodsSaveBo {

    @Schema(description = "商品业务编号，留空则自动生成")
    private String goodsNo;

    @Schema(description = "商品名称")
    @NotBlank(message = "商品名称不能为空")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "所属分类ID")
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @Schema(description = "商品详情描述")
    private String detail;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "使用教程视频URL")
    private String tutorialVideoUrl;

    @Schema(description = "日租价（元）")
    @NotNull(message = "日租价不能为空")
    private BigDecimal rentPriceDay;

    @Schema(description = "时租价（元）")
    private BigDecimal rentPriceHour;

    @Schema(description = "过夜租价（元）")
    private BigDecimal rentPriceOvernight;

    @Schema(description = "过夜租开始时间 HH:mm")
    private String overnightStartTime;

    @Schema(description = "过夜租结束时间 HH:mm")
    private String overnightEndTime;

    @Schema(description = "押金（元）")
    @NotNull(message = "押金不能为空")
    private BigDecimal depositAmount;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：1上架 0下架，默认下架")
    private Integer status;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "总库存（默认规格）")
    private Integer totalStock;

    @Schema(description = "租赁规格列表（可选 SKU）")
    @Valid
    private List<RentGoodsSpecItemBo> specs;

    @Schema(description = "商品参数列表（详情展示）")
    @Valid
    private List<RentGoodsSpecItemBo> params;

    @Schema(description = "配件套餐列表")
    @Valid
    private List<RentGoodsPackageItemBo> packages;

    @Schema(description = "轮播图列表")
    private List<String> images;
}
