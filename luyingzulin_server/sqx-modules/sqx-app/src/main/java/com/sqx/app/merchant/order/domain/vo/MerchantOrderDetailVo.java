package com.sqx.app.merchant.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户订单详情
 */
@Data
@Schema(description = "商户订单详情")
public class MerchantOrderDetailVo {

    @Schema(description = "商户订单ID")
    private Long id;

    @Schema(description = "商户订单号")
    private String merchantOrderNo;

    @Schema(description = "关联用户租赁订单ID")
    private Long rentOrderId;

    @Schema(description = "关联用户租赁订单号")
    private String rentOrderNo;

    @Schema(description = "用户侧订单状态")
    private String rentOrderStatus;

    @Schema(description = "用户侧订单状态文案")
    private String rentOrderStatusLabel;

    @Schema(description = "商户侧状态")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "Tab 分类")
    private String statusType;

    @Schema(description = "用户昵称")
    private String userName;

    @Schema(description = "商品摘要")
    private String goodsName;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "订单押金")
    private BigDecimal depositAmount;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人手机号")
    private String contactPhone;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "取货方式文案")
    private String pickupTypeLabel;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "备货时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime prepareTime;

    @Schema(description = "核销时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pickupTime;

    @Schema(description = "完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completeTime;

    @Schema(description = "可执行操作：prepare/pickup/inspection/none")
    private String nextAction;

    @Schema(description = "时间轴")
    private List<MerchantOrderTimelineVo> timeline;

    @Schema(description = "归还预约日期")
    private String returnDate;

    @Schema(description = "归还方式")
    private String returnMethod;

    @Schema(description = "归还方式文案")
    private String returnMethodLabel;

    @Schema(description = "归还备注")
    private String returnRemark;

    @Schema(description = "归还图片")
    private List<String> returnImageUrls;

    @Schema(description = "租赁类型 day/hour/overnight")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Schema(description = "租期天数")
    private Integer days;

    @Schema(description = "封面图（首商品）")
    private String coverImage;

    @Schema(description = "商品明细")
    private List<MerchantOrderGoodsItemVo> items;

    @Schema(description = "验收说明")
    private String inspectionRemark;

    @Schema(description = "验收图片")
    private List<String> inspectionImageUrls;

    @Schema(description = "扣款金额")
    private BigDecimal deductAmount;

    @Schema(description = "押金退款金额")
    private BigDecimal refundAmount;
}
