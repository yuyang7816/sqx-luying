package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端-租赁订单详情
 */
@Data
@Schema(description = "租赁订单详情")
public class RentOrderDetailVo {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickName;

    @Schema(description = "用户手机号")
    private String userPhone;

    @Schema(description = "租赁类型")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "租赁天数")
    private Integer days;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "取货码")
    private String pickupCode;

    @Schema(description = "租金合计")
    private BigDecimal rentAmount;

    @Schema(description = "押金合计")
    private BigDecimal depositAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryAmount;

    @Schema(description = "优惠金额")
    private BigDecimal couponAmount;

    @Schema(description = "应付总额")
    private BigDecimal payAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime returnTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @Schema(description = "商品明细")
    private List<RentOrderItemVo> items;

    @Schema(description = "状态时间轴")
    private List<RentOrderLogVo> logs;
}
