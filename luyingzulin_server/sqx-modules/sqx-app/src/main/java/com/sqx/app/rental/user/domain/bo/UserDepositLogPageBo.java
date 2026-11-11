package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserDepositLogPageBo extends PageParam {

    @Schema(description = "流水类型：pay/freeze/unfreeze/refund/deduct")
    private String logType;

    @Schema(description = "租赁订单ID")
    private Long orderId;
}
