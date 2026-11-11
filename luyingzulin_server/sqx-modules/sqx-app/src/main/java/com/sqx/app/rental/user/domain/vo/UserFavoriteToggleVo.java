package com.sqx.app.rental.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收藏切换结果
 */
@Data
@Schema(description = "收藏切换结果")
public class UserFavoriteToggleVo {

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "当前是否已收藏")
    private Boolean favorite;

    @Schema(description = "当前收藏总数")
    private Integer favoriteCount;
}
