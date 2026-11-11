package com.sqx.app.rental.stock.service;

import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;

import java.time.LocalDateTime;

/**
 * 租赁库存服务
 */
public interface RentStockService {

    /**
     * 时段库存校验
     */
    RentStockCheckVo check(RentStockCheckBo checkBo);

    /**
     * 下单预占库存（PRELOCK）
     */
    void prelock(Long orderId, String orderNo, Long goodsId, Long specId,
                 LocalDateTime startTime, LocalDateTime endTime, int qty);

    /**
     * 支付成功确认占用（PRELOCK → OCCUPIED）
     */
    void confirmOccupy(Long orderId);

    /**
     * 取消/超时释放预占（PRELOCK → RELEASED）
     */
    void releaseOccupy(Long orderId);

    /**
     * 续租延长库存占用结束时间
     */
    void extendOccupyEndTime(Long orderId, LocalDateTime newEndTime);

    /**
     * 订单完成释放库存占用（OCCUPIED → RELEASED）
     */
    void finishOccupy(Long orderId);
}
