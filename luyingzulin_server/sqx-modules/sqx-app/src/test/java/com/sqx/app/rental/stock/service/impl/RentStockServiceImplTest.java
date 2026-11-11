package com.sqx.app.rental.stock.service.impl;

import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.entity.RentStockOccupy;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.mapper.RentStockOccupyMapper;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M02：时段库存校验单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentStockServiceImplTest {

    @Mock
    private RentGoodsMapper rentGoodsMapper;
    @Mock
    private RentStockMapper rentStockMapper;
    @Mock
    private RentStockOccupyMapper rentStockOccupyMapper;

    @InjectMocks
    private RentStockServiceImpl rentStockService;

    @Test
    void checkShouldReturnAvailableWhenNoOverlapOccupy() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentStock stock = new RentStock();
        stock.setAvailableQty(12);
        when(rentStockMapper.selectOne(any())).thenReturn(stock);
        when(rentStockOccupyMapper.selectList(any())).thenReturn(Collections.emptyList());

        RentStockCheckBo checkBo = buildCheckBo(1);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertTrue(result.getAvailable());
        assertEquals(12, result.getRemainStock());
    }

    @Test
    void checkShouldDeductOverlappingOccupyQty() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentStock stock = new RentStock();
        stock.setAvailableQty(12);
        when(rentStockMapper.selectOne(any())).thenReturn(stock);

        RentStockOccupy occupy = new RentStockOccupy();
        occupy.setQty(2);
        when(rentStockOccupyMapper.selectList(any())).thenReturn(Collections.singletonList(occupy));

        RentStockCheckBo checkBo = buildCheckBo(10);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertTrue(result.getAvailable());
        assertEquals(10, result.getRemainStock());
    }

    @Test
    void checkShouldReturnUnavailableWhenQtyExceedsRemain() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentStock stock = new RentStock();
        stock.setAvailableQty(5);
        when(rentStockMapper.selectOne(any())).thenReturn(stock);
        when(rentStockOccupyMapper.selectList(any())).thenReturn(Collections.emptyList());

        RentStockCheckBo checkBo = buildCheckBo(6);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertFalse(result.getAvailable());
        assertEquals(5, result.getRemainStock());
    }

    @Test
    void checkShouldReturnUnavailableWhenOverlapOccupyLeavesInsufficientStock() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentStock stock = new RentStock();
        stock.setAvailableQty(12);
        when(rentStockMapper.selectOne(any())).thenReturn(stock);

        RentStockOccupy occupy = new RentStockOccupy();
        occupy.setQty(2);
        when(rentStockOccupyMapper.selectList(any())).thenReturn(Collections.singletonList(occupy));

        RentStockCheckBo checkBo = buildCheckBo(11);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertFalse(result.getAvailable());
        assertEquals(10, result.getRemainStock());
    }

    @Test
    void checkShouldFallbackToDefaultSpecStockWhenSpecRecordMissing() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentStock stock = new RentStock();
        stock.setSpecId(0L);
        stock.setAvailableQty(8);
        when(rentStockMapper.selectOne(any())).thenReturn(null, stock);
        when(rentStockOccupyMapper.selectList(any())).thenReturn(Collections.emptyList());

        RentStockCheckBo checkBo = buildCheckBo(1);
        checkBo.setSpecId(15L);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertTrue(result.getAvailable());
        assertEquals(8, result.getRemainStock());
    }

    @Test
    void checkShouldReturnUnavailableWhenStockRecordMissing() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(1);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);
        when(rentStockMapper.selectOne(any())).thenReturn(null);

        RentStockCheckBo checkBo = buildCheckBo(1);
        RentStockCheckVo result = rentStockService.check(checkBo);

        assertFalse(result.getAvailable());
        assertEquals(0, result.getRemainStock());
    }

    @Test
    void checkShouldThrowWhenEndTimeNotAfterStartTime() {
        RentStockCheckBo checkBo = buildCheckBo(1);
        checkBo.setEndTime(checkBo.getStartTime());

        assertThrows(BizException.class, () -> rentStockService.check(checkBo));
    }

    @Test
    void checkShouldThrowWhenGoodsNotOnShelf() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setStatus(0);
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        assertThrows(BizException.class, () -> rentStockService.check(buildCheckBo(1)));
    }

    private static RentStockCheckBo buildCheckBo(int quantity) {
        RentStockCheckBo checkBo = new RentStockCheckBo();
        checkBo.setGoodsId(1L);
        checkBo.setSpecId(0L);
        checkBo.setStartTime(LocalDateTime.of(2026, 8, 10, 10, 0));
        checkBo.setEndTime(LocalDateTime.of(2026, 8, 12, 18, 0));
        checkBo.setQuantity(quantity);
        return checkBo;
    }
}
