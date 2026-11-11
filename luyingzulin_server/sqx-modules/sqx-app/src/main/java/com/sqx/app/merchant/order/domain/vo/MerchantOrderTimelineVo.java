package com.sqx.app.merchant.order.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商户订单时间轴节点
 */
@Data
@Schema(description = "商户订单时间轴节点")
public class MerchantOrderTimelineVo {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "时间")
    private String time;
}
