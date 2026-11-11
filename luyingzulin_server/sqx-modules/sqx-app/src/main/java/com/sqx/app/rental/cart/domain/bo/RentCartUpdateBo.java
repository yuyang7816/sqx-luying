package com.sqx.app.rental.cart.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 更新购物车
 */
@Data
@Schema(description = "更新购物车")
public class RentCartUpdateBo {

    @NotNull(message = "购物车ID不能为空")
    @Schema(description = "购物车ID")
    private Long id;

    @Schema(description = "规格ID")
    private Long specId;

    @Schema(description = "配件套餐ID")
    private Long packageId;

    @Schema(description = "租赁类型：day/hour/overnight")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Min(value = 1, message = "数量至少为1")
    @Schema(description = "数量")
    private Integer qty;
}
