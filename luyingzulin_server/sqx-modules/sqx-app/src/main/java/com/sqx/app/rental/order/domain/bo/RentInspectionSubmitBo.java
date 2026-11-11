package com.sqx.app.rental.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商户录入验收
 */
@Data
@Schema(description = "商户录入验收")
public class RentInspectionSubmitBo {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "设备状况：good/minor_damage/major_damage")
    private String deviceCondition;

    @Schema(description = "扣款金额")
    private BigDecimal deductAmount;

    @Schema(description = "验收图片路径列表（File Upload 组件上传后的路径）")
    private List<String> imageUrls;

    @Schema(description = "验收备注")
    private String remark;
}
