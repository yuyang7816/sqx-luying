package com.sqx.app.merchant.order.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商户订单分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "商户订单分页入参")
public class MerchantOrderPageBo extends PageParam {

    @Schema(description = "Tab：all/pending/running/review/completed")
    private String tab;

    @Schema(description = "关键词：订单号/联系人/商品名")
    private String keyword;

    @Schema(description = "商户订单号（LINK-xxx）")
    private String merchantOrderNo;

    @Schema(description = "用户租赁订单号")
    private String rentOrderNo;
}
