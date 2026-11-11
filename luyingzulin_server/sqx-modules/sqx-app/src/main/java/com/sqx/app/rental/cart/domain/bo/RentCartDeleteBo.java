package com.sqx.app.rental.cart.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量删除购物车
 */
@Data
@Schema(description = "批量删除购物车")
public class RentCartDeleteBo {

    @NotEmpty(message = "购物车ID不能为空")
    @Schema(description = "购物车ID列表")
    private List<Long> ids;
}
