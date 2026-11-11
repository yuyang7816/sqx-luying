package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplateSaveBo {

    @Schema(description = "模板ID（编辑时必填）")
    private Long id;

    @NotBlank(message = "模板编号不能为空")
    @Schema(description = "模板编号")
    private String templateNo;

    @NotBlank(message = "名称不能为空")
    @Schema(description = "优惠券名称")
    private String name;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "类型")
    private String couponType;

    @NotNull(message = "面额不能为空")
    @Schema(description = "面额")
    private BigDecimal amount;

    @Schema(description = "最低消费门槛")
    private BigDecimal minAmount;

    @Schema(description = "折扣率")
    private BigDecimal discountRate;

    @Schema(description = "有效期开始")
    private LocalDateTime validStart;

    @Schema(description = "有效期结束")
    private LocalDateTime validEnd;

    @Schema(description = "适用范围")
    private String scopeType;

    @Schema(description = "范围值")
    private String scopeValue;

    @Schema(description = "状态：1启用 0停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
