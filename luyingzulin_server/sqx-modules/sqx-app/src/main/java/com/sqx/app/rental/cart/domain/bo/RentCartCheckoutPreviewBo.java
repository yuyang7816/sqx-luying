package com.sqx.app.rental.cart.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 结算预览
 */
@Data
@Schema(description = "结算预览")
public class RentCartCheckoutPreviewBo {

    @Schema(description = "购物车ID列表，为空则取已勾选项")
    private List<Long> cartIds;

    @NotBlank(message = "取货方式不能为空")
    @Schema(description = "取货方式：store/pickup_point/delivery")
    private String pickupType;

    @Schema(description = "自提门店ID")
    private Long pickupStoreId;

    @Schema(description = "配送地址ID")
    private Long addressId;

    @Schema(description = "优惠券ID")
    private Long couponId;
}
