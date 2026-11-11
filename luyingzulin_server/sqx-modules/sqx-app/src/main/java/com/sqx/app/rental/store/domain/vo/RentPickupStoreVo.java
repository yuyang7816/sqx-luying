package com.sqx.app.rental.store.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * C 端取货门店/自提点
 */
@Getter
@Setter
public class RentPickupStoreVo {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "所属商户ID")
    private Long merchantId;

    @Schema(description = "门店编号")
    private String storeNo;

    @Schema(description = "门店名称")
    private String name;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "基础配送费（元）")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "满额免配送门槛（元，仅按租金计，不含押金）")
    private BigDecimal freeDeliveryThreshold;

    @Schema(description = "取货方式：store/pickup_point")
    private String pickupType;
}
