package com.sqx.app.rental.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 租赁商品列表项
 */
@Data
@Schema(description = "租赁商品列表项")
public class RentGoodsListVo {

    @Schema(description = "商品ID")
    private Long id;

    @Schema(description = "商品业务编号")
    private String goodsNo;

    @Schema(description = "商品名称")
    private String name;

    @Schema(description = "副标题")
    private String subtitle;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "使用教程视频URL")
    private String tutorialVideoUrl;

    @Schema(description = "日租价")
    private BigDecimal rentPriceDay;

    @Schema(description = "时租价")
    private BigDecimal rentPriceHour;

    @Schema(description = "过夜租价")
    private BigDecimal rentPriceOvernight;

    @Schema(description = "过夜租开始时间 HH:mm")
    private String overnightStartTime;

    @Schema(description = "过夜租结束时间 HH:mm")
    private String overnightEndTime;

    @Schema(description = "当前租赁模式展示价（随 rentType 筛选变化）")
    private BigDecimal rentPrice;

    @Schema(description = "押金")
    private BigDecimal depositAmount;

    @Schema(description = "销量")
    private Integer salesCount;

    @Schema(description = "可用库存")
    private Integer availableStock;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "上架状态 0下架 1上架")
    private Integer status;

    @Schema(description = "所属商户ID")
    private Long merchantId;

    @Schema(description = "所属商户名称")
    private String merchantName;

    @Schema(description = "标签列表")
    private List<String> tags;
}
