package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 续租预览
 */
@Data
@Schema(description = "续租预览")
public class RentRenewPreviewVo {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "续租计费小时数")
    private Integer extraHours;

    @Schema(description = "时租单价")
    private BigDecimal hourUnitPrice;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "当前租期结束")
    private LocalDateTime currentEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "续租后结束时间")
    private LocalDateTime newEndTime;

    @Schema(description = "续租租金")
    private BigDecimal renewAmount;
}
