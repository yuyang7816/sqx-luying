package com.sqx.app.rental.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建续租
 */
@Data
@Schema(description = "创建续租")
public class RentRenewCreateBo {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @NotBlank(message = "续租结束时间不能为空")
    @Schema(description = "续租结束时间 yyyy-MM-dd HH:mm:ss")
    private String newEndTime;

    @Schema(description = "续租方式 day/hour/overnight，仅用于前端展示记录")
    private String renewRentType;
}
