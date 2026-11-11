package com.sqx.app.merchant.apply.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 入驻进度时间轴节点
 */
@Data
@Schema(description = "入驻进度时间轴节点")
public class MerchantApplyTimelineVo {

    @Schema(description = "节点标题")
    private String title;

    @Schema(description = "节点时间")
    private String time;

    @Schema(description = "节点说明")
    private String desc;
}
