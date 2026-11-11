package com.sqx.app.rental.stock.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 时段库存校验入参
 */
@Data
@Schema(description = "时段库存校验入参")
public class RentStockCheckBo {

    @Schema(description = "商品ID")
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    @Schema(description = "规格ID，默认0表示默认规格")
    private Long specId;

    @Schema(description = "租期开始时间")
    @NotNull(message = "租期开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "租期结束时间")
    @NotNull(message = "租期结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "租赁数量")
    @NotNull(message = "租赁数量不能为空")
    @Min(value = 1, message = "租赁数量至少为1")
    private Integer quantity;
}
