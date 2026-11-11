package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端-租赁订单列表项
 */
@Data
@Schema(description = "租赁订单列表项")
public class RentOrderListVo {

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

    @Schema(description = "商品摘要")
    private String goodsSummary;

    @Schema(description = "首件商品封面图")
    private String coverImage;

    @Schema(description = "租赁类型")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Schema(description = "租赁天数")
    private Integer days;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "应付总额")
    private BigDecimal payAmount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间")
    private LocalDateTime createTime;

    @Schema(description = "订单明细")
    private List<RentOrderItemVo> items;
}
