package com.sqx.app.merchant.store.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理端门店列表项
 */
@Data
@Schema(description = "管理端门店列表项")
public class AdminMerchantStoreListVo {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "门店编号")
    private String storeNo;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "商户名称")
    private String merchantName;

    @Schema(description = "门店名称")
    private String name;

    @Schema(description = "门店地址")
    private String address;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
