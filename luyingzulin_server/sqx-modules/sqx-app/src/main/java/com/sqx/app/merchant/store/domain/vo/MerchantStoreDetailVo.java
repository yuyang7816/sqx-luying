package com.sqx.app.merchant.store.domain.vo;

import com.sqx.app.merchant.staff.domain.vo.MerchantStaffVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商户门店详情（含员工列表）
 */
@Data
@Schema(description = "商户门店详情")
public class MerchantStoreDetailVo {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "门店编号")
    private String storeNo;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "门店名称")
    private String name;

    @Schema(description = "地址")
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

    @Schema(description = "是否支持配送")
    private Integer deliveryEnabled;

    @Schema(description = "是否支持自提")
    private Integer selfPickupEnabled;

    @Schema(description = "基础配送费")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "免配送门槛")
    private BigDecimal freeDeliveryThreshold;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "员工列表")
    private List<MerchantStaffVo> staffList;
}
