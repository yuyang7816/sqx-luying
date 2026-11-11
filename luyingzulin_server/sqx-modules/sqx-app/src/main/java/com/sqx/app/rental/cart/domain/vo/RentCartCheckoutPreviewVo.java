package com.sqx.app.rental.cart.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算预览结果（含 confirmPayload）
 */
@Data
@Schema(description = "结算预览结果")
public class RentCartCheckoutPreviewVo {

    @Schema(description = "租金合计")
    private BigDecimal rentAmount;

    @Schema(description = "押金合计")
    private BigDecimal depositAmount;

    @Schema(description = "配送费")
    private BigDecimal deliveryAmount;

    @Schema(description = "配送费规则说明")
    private String deliveryFeeRemark;

    @Schema(description = "优惠金额")
    private BigDecimal couponAmount;

    @Schema(description = "应付总额")
    private BigDecimal payAmount;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "自提门店ID")
    private Long pickupStoreId;

    @Schema(description = "配送地址ID")
    private Long addressId;

    @Schema(description = "预览商品明细")
    private List<RentCartCheckoutItemVo> items;

    @Schema(description = "确认下单载荷，可直接传给 order/create")
    private ConfirmPayload confirmPayload;

    @Data
    @Schema(description = "结算预览商品行")
    public static class RentCartCheckoutItemVo {

        @Schema(description = "购物车ID")
        private Long cartId;

        @Schema(description = "商品ID")
        private Long goodsId;

        @Schema(description = "商品名称")
        private String goodsName;

        @Schema(description = "规格ID")
        private Long specId;

        @Schema(description = "配件套餐ID")
        private Long packageId;

        @Schema(description = "租赁类型")
        private String rentType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        @Schema(description = "租赁天数")
        private Integer days;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "租金小计")
        private BigDecimal itemRentAmount;

        @Schema(description = "押金小计")
        private BigDecimal itemDepositAmount;
    }

    @Data
    @Schema(description = "确认下单载荷")
    public static class ConfirmPayload {

        @Schema(description = "商品行")
        private List<ConfirmItem> items;

        @Schema(description = "取货方式")
        private String pickupType;

        @Schema(description = "自提门店ID")
        private Long pickupStoreId;

        @Schema(description = "配送地址ID")
        private Long addressId;

        @Schema(description = "优惠券ID")
        private Long couponId;

        @Schema(description = "关联购物车ID")
        private List<Long> cartIds;

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
    }

    @Data
    @Schema(description = "确认下单商品行")
    public static class ConfirmItem {

        @Schema(description = "商品ID")
        private Long goodsId;

        @Schema(description = "规格ID")
        private Long specId;

        @Schema(description = "配件套餐ID")
        private Long packageId;

        @Schema(description = "租赁类型")
        private String rentType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        @Schema(description = "租赁天数")
        private Integer days;

        @Schema(description = "数量")
        private Integer quantity;
    }
}
