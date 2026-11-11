package com.sqx.app.rental.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 取货凭证
 */
@Data
@Schema(description = "取货凭证")
public class RentPickupCodeVo {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "取货码")
    private String pickupCode;

    @Schema(description = "取货码展示格式（带空格）")
    private String pickupCodeDisplay;

    @Schema(description = "是否已使用")
    private Boolean used;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "取货方式")
    private String pickupType;

    @Schema(description = "自提门店ID")
    private Long pickupStoreId;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "门店地址")
    private String storeAddress;

    @Schema(description = "门店电话")
    private String storePhone;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "完整地址（省市区+详细地址）")
    private String fullAddress;

    @Schema(description = "纬度")
    private java.math.BigDecimal latitude;

    @Schema(description = "经度")
    private java.math.BigDecimal longitude;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "门头照")
    private String facadeImage;

    @Schema(description = "订单状态")
    private String orderStatus;

    @Schema(description = "是否已核销取货")
    private Boolean verified;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;
}
