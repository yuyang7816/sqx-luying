package com.sqx.app.rental.dashboard.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "订单状态分布项")
public class AdminRentDashboardStatusItemVo {

    @Schema(description = "状态码")
    private String status;

    @Schema(description = "状态名称")
    private String label;

    @Schema(description = "数量")
    private Integer count;
}
