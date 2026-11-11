package com.sqx.app.rental.stock.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 时段库存校验结果
 */
@Data
@Schema(description = "时段库存校验结果")
public class RentStockCheckVo {

    @Schema(description = "是否可租")
    private Boolean available;

    @Schema(description = "剩余可租数量")
    private Integer remainStock;
}
