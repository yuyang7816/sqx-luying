package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 归还详情
 */
@Data
@Schema(description = "归还详情")
public class RentReturnDetailVo {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "归还方式")
    private String returnMethod;

    @Schema(description = "计划归还日期")
    private String returnDate;

    @Schema(description = "归还备注")
    private String remark;

    @Schema(description = "归还图片")
    private List<String> imageUrls;

    @Schema(description = "申请状态")
    private String applyStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "申请时间")
    private LocalDateTime applyTime;
}
