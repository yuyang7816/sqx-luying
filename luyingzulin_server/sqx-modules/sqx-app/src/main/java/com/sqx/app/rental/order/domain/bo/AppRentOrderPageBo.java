package com.sqx.app.rental.order.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户端-租赁订单分页查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户端租赁订单分页查询")
public class AppRentOrderPageBo extends PageParam {

    @Schema(description = "订单状态 Tab：pending_payment/waiting_pickup/renting/returning/completed 等")
    private String status;
}
