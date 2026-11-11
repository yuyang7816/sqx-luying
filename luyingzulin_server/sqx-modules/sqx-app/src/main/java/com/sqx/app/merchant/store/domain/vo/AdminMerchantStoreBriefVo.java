package com.sqx.app.merchant.store.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端商户详情中的门店摘要
 */
@Data
@Schema(description = "门店摘要")
public class AdminMerchantStoreBriefVo {

    @Schema(description = "门店ID")
    private Long id;

    @Schema(description = "门店编号")
    private String storeNo;

    @Schema(description = "门店名称")
    private String name;

    @Schema(description = "门店地址")
    private String address;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;
}
