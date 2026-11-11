package com.sqx.app.rental.user.service.impl;

import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.user.domain.entity.UserFavorite;
import com.sqx.app.rental.user.domain.vo.UserFavoriteToggleVo;
import com.sqx.app.rental.user.domain.vo.UserFavoriteVo;
import com.sqx.app.rental.user.mapper.UserFavoriteMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * M05：收藏列表组装与 toggle 单元测试
 */
@ExtendWith(MockitoExtension.class)
class UserFavoriteServiceImplM05Test {

    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private RentGoodsMapper rentGoodsMapper;

    @InjectMocks
    private UserFavoriteServiceImpl userFavoriteService;

    @Test
    void listShouldJoinGoodsBusinessFields() {
        UserFavorite favorite = new UserFavorite();
        favorite.setId(10L);
        favorite.setUserId(1L);
        favorite.setGoodsId(2L);

        RentGoods goods = new RentGoods();
        goods.setId(2L);
        goods.setGoodsNo("G002");
        goods.setName("双人帐篷");
        goods.setCoverImage("/img/tent.jpg");
        goods.setRentPriceDay(new BigDecimal("99.00"));
        goods.setDepositAmount(new BigDecimal("200.00"));
        goods.setStatus(1);

        when(userFavoriteMapper.selectList(any())).thenReturn(Collections.singletonList(favorite));
        when(rentGoodsMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(goods));

        List<UserFavoriteVo> result = userFavoriteService.list(1L);

        assertEquals(1, result.size());
        assertEquals("G002", result.get(0).getGoodsNo());
        assertEquals("双人帐篷", result.get(0).getGoodsName());
        assertEquals(new BigDecimal("99.00"), result.get(0).getRentPriceDay());
    }

    @Test
    void toggleShouldInsertWhenNotFavorite() {
        RentGoods goods = new RentGoods();
        goods.setId(3L);
        when(rentGoodsMapper.selectById(3L)).thenReturn(goods);
        when(userFavoriteMapper.selectOne(any())).thenReturn(null);
        when(userFavoriteMapper.selectCount(any())).thenReturn(1L);

        UserFavoriteToggleVo vo = userFavoriteService.toggle(1L, 3L);

        assertTrue(vo.getFavorite());
        assertEquals(1, vo.getFavoriteCount());
        verify(userFavoriteMapper).insert(any(UserFavorite.class));
    }

    @Test
    void doubleToggleShouldRestoreOriginalFavoriteState() {
        RentGoods goods = new RentGoods();
        goods.setId(3L);
        UserFavorite existing = new UserFavorite();
        existing.setId(99L);
        existing.setGoodsId(3L);

        when(rentGoodsMapper.selectById(3L)).thenReturn(goods);
        when(userFavoriteMapper.selectOne(any())).thenReturn(null, existing);
        when(userFavoriteMapper.selectCount(any())).thenReturn(1L, 0L);

        UserFavoriteToggleVo added = userFavoriteService.toggle(1L, 3L);
        UserFavoriteToggleVo removed = userFavoriteService.toggle(1L, 3L);

        assertTrue(added.getFavorite());
        assertFalse(removed.getFavorite());
        verify(userFavoriteMapper).insert(any(UserFavorite.class));
        verify(userFavoriteMapper).deleteById(99L);
    }

    @Test
    void toggleShouldDeleteWhenAlreadyFavorite() {
        RentGoods goods = new RentGoods();
        goods.setId(3L);
        UserFavorite existing = new UserFavorite();
        existing.setId(99L);
        existing.setGoodsId(3L);

        when(rentGoodsMapper.selectById(3L)).thenReturn(goods);
        when(userFavoriteMapper.selectOne(any())).thenReturn(existing);
        when(userFavoriteMapper.selectCount(any())).thenReturn(0L);

        UserFavoriteToggleVo vo = userFavoriteService.toggle(1L, 3L);

        assertFalse(vo.getFavorite());
        verify(userFavoriteMapper).deleteById(99L);
    }
}
