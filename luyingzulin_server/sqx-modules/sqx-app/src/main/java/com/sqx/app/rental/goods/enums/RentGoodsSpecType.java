package com.sqx.app.rental.goods.enums;

/**
 * 商品规格类型
 */
public final class RentGoodsSpecType {

    /** 租赁规格（可选 SKU，含加价） */
    public static final int SKU = 1;

    /** 商品参数（详情展示，不参与下单） */
    public static final int PARAM = 2;

    private RentGoodsSpecType() {
    }
}
