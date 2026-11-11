package com.sqx.app.merchant.apply.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 入驻进度 VO
 */
@Data
@Schema(description = "入驻进度")
public class MerchantApplyProgressVo {

    @Schema(description = "申请ID")
    private Long applyId;

    @Schema(description = "申请编号")
    private String applyNo;

    @Schema(description = "状态：pending/reviewing/approved/rejected")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "商家名称")
    private String merchantName;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "负责人")
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

    @Schema(description = "证照图片")
    private List<String> licenseUrls;

    @Schema(description = "补充图片（门头封面、轮播图等）")
    private List<String> extraUrls;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "审核通过后商户ID")
    private Long merchantId;

    @Schema(description = "审核通过后门店ID")
    private Long storeId;

    @Schema(description = "审核时间轴")
    private List<MerchantApplyTimelineVo> timeline;
}
