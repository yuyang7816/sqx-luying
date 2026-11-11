package com.sqx.app.vip.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会员支付状态")
public class VipPayStatusVo {

    @Schema(description = "会员订单号")
    private String orderNo;

    @Schema(description = "是否已支付")
    private Boolean paid;

    @Schema(description = "订单状态")
    private String status;
}
