package com.sqx.app.merchant.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商户订单列表项
 */
@Data
@Schema(description = "商户订单列表项")
public class MerchantOrderListVo {

    @Schema(description = "商户订单ID")
    private Long id;

    @Schema(description = "商户订单号（LINK-{rentOrderId}）")
    private String merchantOrderNo;

    @Schema(description = "关联用户租赁订单ID")
    private Long rentOrderId;

    @Schema(description = "关联用户租赁订单号")
    private String rentOrderNo;

    @Schema(description = "用户昵称/联系人")
    private String userName;

    @Schema(description = "商品摘要")
    private String goodsName;

    @Schema(description = "商户侧状态")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "Tab 分类：pending/running/review/completed")
    private String statusType;

    @Schema(description = "实付金额")
    private BigDecimal payAmount;

    @Schema(description = "联系人手机号")
    private String contactPhone;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "取货方式文案")
    private String pickupTypeLabel;

    @Schema(description = "地址摘要")
    private String address;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "租赁类型 day/hour/overnight")
    private String rentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期开始")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "租期结束")
    private LocalDateTime endTime;

    @Schema(description = "封面图（首商品）")
    private String coverImage;

    @Schema(description = "商品明细")
    private List<MerchantOrderGoodsItemVo> items;

    @Schema(description = "可执行操作：prepare/pickup/inspection/none")
    private String nextAction;
}
