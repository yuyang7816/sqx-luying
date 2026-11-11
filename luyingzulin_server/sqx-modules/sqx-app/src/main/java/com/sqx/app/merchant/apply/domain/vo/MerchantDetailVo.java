package com.sqx.app.merchant.apply.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sqx.app.merchant.store.domain.vo.AdminMerchantStoreBriefVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端商户详情
 */
@Data
@Schema(description = "管理端商户详情")
public class MerchantDetailVo {

    @Schema(description = "商户ID")
    private Long id;

    @Schema(description = "商户编号")
    private String merchantNo;

    @Schema(description = "商户名称")
    private String name;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "负责人用户ID")
    private Long userId;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "状态文案")
    private String statusLabel;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "关联门店")
    private List<AdminMerchantStoreBriefVo> stores;
}
