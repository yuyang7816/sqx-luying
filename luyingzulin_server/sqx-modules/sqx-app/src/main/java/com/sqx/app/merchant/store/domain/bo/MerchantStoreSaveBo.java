package com.sqx.app.merchant.store.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 商户门店保存入参
 */
@Data
@Schema(description = "商户门店保存入参")
public class MerchantStoreSaveBo {

    @Schema(description = "门店ID，编辑时必填")
    private Long id;

    @Schema(description = "门店名称")
    @NotBlank(message = "门店名称不能为空")
    private String name;

    @Schema(description = "门店地址")
    @NotBlank(message = "门店地址不能为空")
    private String address;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "服务区域")
    private String serviceArea;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "是否支持配送：1是 0否")
    private Integer deliveryEnabled;

    @Schema(description = "是否支持自提：1是 0否")
    private Integer selfPickupEnabled;

    @Schema(description = "基础配送费")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "免配送门槛")
    private BigDecimal freeDeliveryThreshold;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
