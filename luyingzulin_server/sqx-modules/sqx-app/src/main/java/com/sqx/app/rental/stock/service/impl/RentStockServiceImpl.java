package com.sqx.app.rental.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.rental.enums.StockOccupyStatus;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.entity.RentStockOccupy;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.mapper.RentStockOccupyMapper;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 租赁库存服务实现
 */
@Service
public class RentStockServiceImpl implements RentStockService {

    private static final List<String> ACTIVE_OCCUPY_STATUS = Arrays.asList(
            StockOccupyStatus.PRELOCK, StockOccupyStatus.OCCUPIED);

    private static final long DEFAULT_SPEC_ID = 0L;

    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentStockMapper rentStockMapper;
    @Resource
    private RentStockOccupyMapper rentStockOccupyMapper;

    @Override
    public RentStockCheckVo check(RentStockCheckBo checkBo) {
        if (checkBo.getEndTime().isBefore(checkBo.getStartTime())
                || checkBo.getEndTime().isEqual(checkBo.getStartTime())) {
            throw new BizException("租期结束时间必须晚于开始时间");
        }

        RentGoods goods = rentGoodsMapper.selectById(checkBo.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品不存在或未上架");
        }

        StockLookup lookup = resolveStockLookup(checkBo.getGoodsId(), checkBo.getSpecId());
        if (lookup.getStock() == null) {
            RentStockCheckVo vo = new RentStockCheckVo();
            vo.setAvailable(false);
            vo.setRemainStock(0);
            return vo;
        }

        int occupiedQty = sumOccupiedQty(checkBo.getGoodsId(), lookup,
                checkBo.getStartTime(), checkBo.getEndTime(), null);

        RentStock stock = lookup.getStock();
        int baseAvailable = stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
        int remainStock = Math.max(baseAvailable - occupiedQty, 0);

        RentStockCheckVo vo = new RentStockCheckVo();
        vo.setRemainStock(remainStock);
        vo.setAvailable(remainStock >= checkBo.getQuantity());
        return vo;
    }

    @Override
    public void prelock(Long orderId, String orderNo, Long goodsId, Long specId,
                        LocalDateTime startTime, LocalDateTime endTime, int qty) {
        Long normalizedSpecId = specId == null ? 0L : specId;
        RentStockCheckBo checkBo = new RentStockCheckBo();
        checkBo.setGoodsId(goodsId);
        checkBo.setSpecId(normalizedSpecId);
        checkBo.setStartTime(startTime);
        checkBo.setEndTime(endTime);
        checkBo.setQuantity(qty);
        RentStockCheckVo checkVo = check(checkBo);
        if (!Boolean.TRUE.equals(checkVo.getAvailable())) {
            throw new BizException("库存不足，请调整租期或数量");
        }

        RentStockOccupy occupy = new RentStockOccupy();
        occupy.setGoodsId(goodsId);
        occupy.setSpecId(normalizedSpecId);
        occupy.setOrderId(orderId);
        occupy.setOrderNo(orderNo);
        occupy.setStartTime(startTime);
        occupy.setEndTime(endTime);
        occupy.setQty(qty);
        occupy.setStatus(StockOccupyStatus.PRELOCK);
        rentStockOccupyMapper.insert(occupy);
    }

    @Override
    public void confirmOccupy(Long orderId) {
        rentStockOccupyMapper.update(null, new LambdaUpdateWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getOrderId, orderId)
                .eq(RentStockOccupy::getStatus, StockOccupyStatus.PRELOCK)
                .set(RentStockOccupy::getStatus, StockOccupyStatus.OCCUPIED));
    }

    @Override
    public void releaseOccupy(Long orderId) {
        rentStockOccupyMapper.update(null, new LambdaUpdateWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getOrderId, orderId)
                .eq(RentStockOccupy::getStatus, StockOccupyStatus.PRELOCK)
                .set(RentStockOccupy::getStatus, StockOccupyStatus.RELEASED));
    }

    @Override
    public void extendOccupyEndTime(Long orderId, LocalDateTime newEndTime) {
        if (newEndTime == null) {
            throw new BizException("续租结束时间无效");
        }
        List<RentStockOccupy> occupies = rentStockOccupyMapper.selectList(new LambdaQueryWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getOrderId, orderId)
                .eq(RentStockOccupy::getStatus, StockOccupyStatus.OCCUPIED));
        for (RentStockOccupy occupy : occupies) {
            RentStockCheckBo checkBo = new RentStockCheckBo();
            checkBo.setGoodsId(occupy.getGoodsId());
            checkBo.setSpecId(occupy.getSpecId());
            checkBo.setStartTime(occupy.getEndTime());
            checkBo.setEndTime(newEndTime);
            checkBo.setQuantity(occupy.getQty() == null ? 1 : occupy.getQty());
            RentStockCheckVo checkVo = checkExcludeOrder(checkBo, orderId);
            if (!Boolean.TRUE.equals(checkVo.getAvailable())) {
                throw new BizException("续租时段库存不足");
            }
        }
        rentStockOccupyMapper.update(null, new LambdaUpdateWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getOrderId, orderId)
                .eq(RentStockOccupy::getStatus, StockOccupyStatus.OCCUPIED)
                .set(RentStockOccupy::getEndTime, newEndTime));
    }

    @Override
    public void finishOccupy(Long orderId) {
        rentStockOccupyMapper.update(null, new LambdaUpdateWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getOrderId, orderId)
                .eq(RentStockOccupy::getStatus, StockOccupyStatus.OCCUPIED)
                .set(RentStockOccupy::getStatus, StockOccupyStatus.RELEASED));
    }

    private RentStockCheckVo checkExcludeOrder(RentStockCheckBo checkBo, Long excludeOrderId) {
        RentGoods goods = rentGoodsMapper.selectById(checkBo.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品不存在或未上架");
        }
        StockLookup lookup = resolveStockLookup(checkBo.getGoodsId(), checkBo.getSpecId());
        if (lookup.getStock() == null) {
            RentStockCheckVo vo = new RentStockCheckVo();
            vo.setAvailable(false);
            vo.setRemainStock(0);
            return vo;
        }
        int occupiedQty = sumOccupiedQty(checkBo.getGoodsId(), lookup,
                checkBo.getStartTime(), checkBo.getEndTime(), excludeOrderId);
        RentStock stock = lookup.getStock();
        int baseAvailable = stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
        int remainStock = Math.max(baseAvailable - occupiedQty, 0);
        RentStockCheckVo vo = new RentStockCheckVo();
        vo.setRemainStock(remainStock);
        vo.setAvailable(remainStock >= checkBo.getQuantity());
        return vo;
    }

    private StockLookup resolveStockLookup(Long goodsId, Long specId) {
        Long requestedSpecId = specId == null ? DEFAULT_SPEC_ID : specId;
        RentStock stock = selectStock(goodsId, requestedSpecId);
        if (stock != null) {
            return new StockLookup(stock, requestedSpecId.equals(DEFAULT_SPEC_ID));
        }
        if (!requestedSpecId.equals(DEFAULT_SPEC_ID)) {
            RentStock defaultStock = selectStock(goodsId, DEFAULT_SPEC_ID);
            if (defaultStock != null) {
                return new StockLookup(defaultStock, true);
            }
        }
        return new StockLookup(null, false);
    }

    private RentStock selectStock(Long goodsId, Long specId) {
        return rentStockMapper.selectOne(new LambdaQueryWrapper<RentStock>()
                .eq(RentStock::getGoodsId, goodsId)
                .eq(RentStock::getSpecId, specId)
                .last("LIMIT 1"));
    }

    private int sumOccupiedQty(Long goodsId, StockLookup lookup, LocalDateTime startTime,
                               LocalDateTime endTime, Long excludeOrderId) {
        LambdaQueryWrapper<RentStockOccupy> wrapper = new LambdaQueryWrapper<RentStockOccupy>()
                .eq(RentStockOccupy::getGoodsId, goodsId)
                .in(RentStockOccupy::getStatus, ACTIVE_OCCUPY_STATUS)
                .lt(RentStockOccupy::getStartTime, endTime)
                .gt(RentStockOccupy::getEndTime, startTime);
        if (!lookup.isGoodsLevelPool()) {
            wrapper.eq(RentStockOccupy::getSpecId, lookup.getStock().getSpecId());
        }
        if (excludeOrderId != null) {
            wrapper.ne(RentStockOccupy::getOrderId, excludeOrderId);
        }
        List<RentStockOccupy> occupies = rentStockOccupyMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(occupies)) {
            return 0;
        }
        return occupies.stream().mapToInt(item -> item.getQty() == null ? 0 : item.getQty()).sum();
    }

    private static final class StockLookup {
        private final RentStock stock;
        private final boolean goodsLevelPool;

        private StockLookup(RentStock stock, boolean goodsLevelPool) {
            this.stock = stock;
            this.goodsLevelPool = goodsLevelPool;
        }

        private RentStock getStock() {
            return stock;
        }

        private boolean isGoodsLevelPool() {
            return goodsLevelPool;
        }
    }
}
