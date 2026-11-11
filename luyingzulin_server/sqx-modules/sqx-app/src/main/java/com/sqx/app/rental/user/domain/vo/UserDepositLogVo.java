package com.sqx.app.rental.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserDepositLogVo {

    private Long id;
    private String logNo;
    private String logType;
    private String logTypeLabel;
    private BigDecimal amount;
    private String orderNo;
    @Schema(description = "关联租赁订单ID")
    private Long orderId;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
