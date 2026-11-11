package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 收藏切换
 */
@Data
@Schema(description = "收藏切换")
public class UserFavoriteToggleBo {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long goodsId;
}
