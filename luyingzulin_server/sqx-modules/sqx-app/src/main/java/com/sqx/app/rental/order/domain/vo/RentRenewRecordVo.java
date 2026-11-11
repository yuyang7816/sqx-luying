package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 续租记录摘要
 */
@Data
@Schema(description = "续租记录摘要")
public class RentRenewRecordVo {

    @Schema(description = "续租单号")
    private String renewNo;

    @Schema(description = "续租天数")
    private Integer extraDays;

    @Schema(description = "续租租金")
    private BigDecimal renewAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "续租前结束时间")
    private LocalDateTime oldEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "续租后结束时间")
    private LocalDateTime newEndTime;

    @Schema(description = "支付状态")
    private String payStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付时间")
    private LocalDateTime payTime;
}
