package com.sqx.app.rental.goods.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品参数（详情展示）
 */
@Data
@Schema(description = "商品参数")
public class RentGoodsParamVo {

    @Schema(description = "参数名称")
    private String name;

    @Schema(description = "参数值")
    private String value;
}
