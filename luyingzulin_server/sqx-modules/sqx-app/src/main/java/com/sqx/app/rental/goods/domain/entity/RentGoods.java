package com.sqx.app.rental.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 租赁商品
 */
@Getter
@Setter
@TableName("rent_goods")
@Schema(name = "RentGoods", description = "租赁商品")
public class RentGoods implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "商品ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属分类ID")
    @TableField("category_id")
    private Long categoryId;

    @Schema(description = "商品业务编号")
    @TableField("goods_no")
    private String goodsNo;

    @Schema(description = "商品名称")
    @TableField("name")
    private String name;

    @Schema(description = "副标题")
    @TableField("subtitle")
    private String subtitle;

    @Schema(description = "商品详情")
    @TableField("detail")
    private String detail;

    @Schema(description = "封面图")
    @TableField("cover_image")
    private String coverImage;

    @Schema(description = "使用教程视频URL")
    @TableField("tutorial_video_url")
    private String tutorialVideoUrl;

    @Schema(description = "日租价")
    @TableField("rent_price_day")
    private BigDecimal rentPriceDay;

    @Schema(description = "时租价")
    @TableField("rent_price_hour")
    private BigDecimal rentPriceHour;

    @Schema(description = "过夜租价")
    @TableField("rent_price_overnight")
    private BigDecimal rentPriceOvernight;

    @Schema(description = "过夜租开始时间 HH:mm")
    @TableField("overnight_start_time")
    private String overnightStartTime;

    @Schema(description = "过夜租结束时间 HH:mm")
    @TableField("overnight_end_time")
    private String overnightEndTime;

    @Schema(description = "押金")
    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @Schema(description = "销量")
    @TableField("sales_count")
    private Integer salesCount;

    @Schema(description = "标签，逗号分隔")
    @TableField("tags")
    private String tags;

    @Schema(description = "状态：1上架 0下架")
    @TableField("status")
    private Integer status;

    @Schema(description = "商户ID")
    @TableField("merchant_id")
    private Long merchantId;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableField("deleted")
    @TableLogic
    private Integer deleted;
}
