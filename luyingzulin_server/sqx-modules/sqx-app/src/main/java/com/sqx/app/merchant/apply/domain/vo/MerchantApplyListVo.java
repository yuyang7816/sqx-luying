package com.sqx.app.merchant.apply.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端入驻申请列表项
 */
@Data
@Schema(description = "管理端入驻申请列表项")
public class MerchantApplyListVo {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "申请编号")
    private String applyNo;

    @Schema(description = "商家名称（同门店名称）")
    private String merchantName;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "门店地址")
    private String storeAddress;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "配送范围")
    private String serviceArea;

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

    @Schema(description = "基础配送费（元）")
    private BigDecimal baseDeliveryFee;

    @Schema(description = "满额免配送门槛（元，仅按租金计，不含押金）")
    private BigDecimal freeDeliveryThreshold;

    @Schema(description = "经营范围")
    private List<String> businessScopes;

    @Schema(description = "资质材料")
    private List<String> licenseUrls;

    @Schema(description = "补充资料（门头封面、轮播图等）")
    private List<String> extraUrls;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "申请时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
}
