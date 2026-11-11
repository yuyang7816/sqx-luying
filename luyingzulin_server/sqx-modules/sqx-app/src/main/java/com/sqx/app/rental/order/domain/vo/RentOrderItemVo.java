package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端-租赁订单明细
 */
@Data
@Schema(description = "租赁订单明细")
public class RentOrderItemVo {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "规格描述")
    private String specInfo;

    @Schema(description = "配件套餐ID")
    private Long packageId;

    @Schema(description = "配件套餐名称")
    private String packageName;

    @Schema(description = "配件套餐描述")
    private String packageDesc;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "租赁类型")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "数量")
    private Integer qty;

    @Schema(description = "租金单价")
    private BigDecimal rentPrice;

    @Schema(description = "押金")
    private BigDecimal depositAmount;

    @Schema(description = "小计")
    private BigDecimal itemAmount;
}
