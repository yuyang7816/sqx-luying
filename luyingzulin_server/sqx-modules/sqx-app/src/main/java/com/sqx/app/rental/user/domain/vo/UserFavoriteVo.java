package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏商品列表项（含商品核心业务字段）
 */
@Data
@Schema(description = "收藏商品列表项")
public class UserFavoriteVo {

    @Schema(description = "收藏ID")
    private Long favoriteId;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品业务编号")
    private String goodsNo;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "封面图")
    private String coverImage;

    @Schema(description = "日租价")
    private BigDecimal rentPriceDay;

    @Schema(description = "押金")
    private BigDecimal depositAmount;

    @Schema(description = "商品状态：1上架 0下架")
    private Integer goodsStatus;

    @Schema(description = "收藏时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime favoriteTime;
}
