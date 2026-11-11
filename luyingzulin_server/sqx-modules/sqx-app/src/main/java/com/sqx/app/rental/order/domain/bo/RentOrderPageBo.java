package com.sqx.app.rental.order.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端-租赁订单分页查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "租赁订单分页查询")
public class RentOrderPageBo extends PageParam {

    @Schema(description = "订单状态：pending_payment/waiting_pickup/renting 等")
    private String status;

    @Schema(description = "商户ID")
    private Long merchantId;

    @Schema(description = "关键词：订单号/用户手机号/商品名/联系人手机")
    private String keyword;

    @Schema(description = "下单开始时间 yyyy-MM-dd HH:mm:ss")
    private String startTime;

    @Schema(description = "下单结束时间 yyyy-MM-dd HH:mm:ss")
    private String endTime;
}
