package com.sqx.app.merchant.apply.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

/**
 * 提交入驻申请入参
 */
@Data
@Schema(description = "提交入驻申请入参")
public class MerchantApplySubmitBo {

    @Schema(description = "商家名称（兼容旧版，未传时使用门店名称）")
    private String merchantName;

    @Schema(description = "联系人姓名")
    @NotBlank(message = "联系人不能为空")
    private String contactName;

    @Schema(description = "联系电话")
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @Schema(description = "门店名称")
    @NotBlank(message = "门店名称不能为空")
    private String storeName;

    @Schema(description = "门店地址")
    @NotBlank(message = "门店地址不能为空")
    private String storeAddress;

    @Schema(description = "省")
    private String province;

    @Schema(description = "市")
    private String city;

    @Schema(description = "区/县")
    private String district;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "配送范围")
    private String serviceArea;

    @Schema(description = "基础配送费（元）")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "满额免配送门槛（元，仅按租金计，不含押金）")
    private BigDecimal freeDeliveryThreshold;

    @Schema(description = "经营范围列表")
    private List<String> businessScopes;

    @Schema(description = "资质材料：营业执照、身份证正反面")
    private List<String> licenseUrls;

    @Schema(description = "补充资料：门头封面、门店轮播图")
    private List<String> extraUrls;
}
