package com.sqx.app.rental.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 提交归还申请
 */
@Data
@Schema(description = "提交归还申请")
public class RentReturnApplyBo {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @NotBlank(message = "归还方式不能为空")
    @Schema(description = "归还方式：store/pickup_point/delivery")
    private String returnMethod;

    @Schema(description = "计划归还日期描述")
    private String returnDate;

    @Schema(description = "归还备注")
    private String remark;

    @Schema(description = "归还图片URL列表")
    private List<String> imageUrls;
}
