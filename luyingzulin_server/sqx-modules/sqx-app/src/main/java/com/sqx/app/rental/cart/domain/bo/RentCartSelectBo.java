package com.sqx.app.rental.cart.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 购物车勾选
 */
@Data
@Schema(description = "购物车勾选")
public class RentCartSelectBo {

    @Schema(description = "购物车ID，单选时使用")
    private Long id;

    @Schema(description = "是否勾选：true勾选 false取消")
    @NotNull(message = "勾选状态不能为空")
    private Boolean selected;

    @Schema(description = "是否全选/全不选")
    private Boolean selectAll;

    @Schema(description = "批量勾选ID列表")
    private List<Long> ids;
}
