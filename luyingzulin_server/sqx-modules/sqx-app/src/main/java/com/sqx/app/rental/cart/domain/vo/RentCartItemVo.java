package com.sqx.app.rental.cart.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车条目
 */
@Data
@Schema(description = "购物车条目")
public class RentCartItemVo {

    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品编号")
    private String goodsNo;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "规格ID")
    private Long specId;

    @Schema(description = "规格描述")
    private String specInfo;

    @Schema(description = "配件套餐ID")
    private Long packageId;

    @Schema(description = "配件套餐名称")
    private String packageName;

    @Schema(description = "租赁类型：day/hour/overnight")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "是否勾选")
    private Boolean selected;

    @Schema(description = "日租价")
    private BigDecimal rentPriceDay;

    @Schema(description = "时租价")
    private BigDecimal rentPriceHour;

    @Schema(description = "过夜租价")
    private BigDecimal rentPriceOvernight;

    @Schema(description = "押金")
    private BigDecimal depositAmount;

    @Schema(description = "小计租金")
    private BigDecimal itemRentAmount;

    @Schema(description = "小计押金")
    private BigDecimal itemDepositAmount;

    @Schema(description = "是否失效（下架/库存不足）")
    private Boolean invalid;

    @Schema(description = "失效原因")
    private String invalidReason;
}
