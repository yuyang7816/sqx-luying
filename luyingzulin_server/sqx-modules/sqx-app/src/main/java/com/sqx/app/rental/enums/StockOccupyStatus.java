package com.sqx.app.rental.enums;

/**
 * 库存占用状态（rent_stock_occupy.status）
 */
public final class StockOccupyStatus {

    /** 下单预占（待支付） */
    public static final String PRELOCK = "PRELOCK";
    /** 支付确认占用 */
    public static final String OCCUPIED = "OCCUPIED";
    /** 已释放 */
    public static final String RELEASED = "RELEASED";

    private StockOccupyStatus() {
    }
}
