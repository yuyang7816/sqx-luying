package com.sqx.app.rental.order.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 创建订单商品行
 */
@Data
@Schema(description = "创建订单商品行")
public class RentOrderCreateItemBo {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "规格ID，默认0")
    private Long specId;

    @Schema(description = "配件套餐ID")
    private Long packageId;

    @NotBlank(message = "租赁类型不能为空")
    @Schema(description = "租赁类型：day/hour/overnight")
    private String rentType;

    @NotNull(message = "租期开始不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @NotNull(message = "租期结束不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Schema(description = "租赁天数")
    private Integer days;

    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "数量")
    private Integer quantity = 1;
}
