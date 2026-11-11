package com.sqx.app.rental.order.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 创建租赁订单
 */
@Data
@Schema(description = "创建租赁订单")
public class RentOrderCreateBo {

    @NotEmpty(message = "商品明细不能为空")
    @Valid
    @Schema(description = "商品明细")
    private List<RentOrderCreateItemBo> items;

    @NotBlank(message = "取货方式不能为空")
    @Schema(description = "取货方式：store/pickup_point/delivery")
    private String pickupType;

    @Schema(description = "自提门店ID")
    private Long pickupStoreId;

    @Schema(description = "配送地址ID")
    private Long addressId;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "优惠券ID")
    private Long couponId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "支付渠道")
    private String payChannel;

    @Schema(description = "关联购物车ID，下单成功后清除")
    private List<Long> cartIds;
}
