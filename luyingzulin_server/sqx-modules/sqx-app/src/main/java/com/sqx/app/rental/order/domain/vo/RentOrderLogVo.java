package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端-租赁订单状态日志
 */
@Data
@Schema(description = "租赁订单状态日志")
public class RentOrderLogVo {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "原状态")
    private String fromStatus;

    @Schema(description = "目标状态")
    private String toStatus;

    @Schema(description = "操作人类型")
    private String operatorType;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "记录时间")
    private LocalDateTime createTime;
}
