package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 验收记录列表项
 */
@Data
@Schema(description = "验收记录列表项")
public class RentInspectionListVo {

    @Schema(description = "验收记录ID")
    private Long id;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "用户昵称")
    private String userNickName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "设备状况")
    private String deviceCondition;

    @Schema(description = "扣款金额")
    private BigDecimal deductAmount;

    @Schema(description = "退还押金")
    private BigDecimal refundAmount;

    @Schema(description = "复核状态")
    private String auditStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "验收时间")
    private LocalDateTime createTime;
}
