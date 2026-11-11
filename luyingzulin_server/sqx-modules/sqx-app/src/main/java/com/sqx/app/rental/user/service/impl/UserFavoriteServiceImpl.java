package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.user.domain.entity.UserFavorite;
import com.sqx.app.rental.user.domain.vo.UserFavoriteToggleVo;
import com.sqx.app.rental.user.domain.vo.UserFavoriteVo;
import com.sqx.app.rental.user.mapper.UserFavoriteMapper;
import com.sqx.app.rental.user.service.UserFavoriteService;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户商品收藏服务实现
 */
@Service
public class UserFavoriteServiceImpl implements UserFavoriteService {

    @Resource
    private UserFavoriteMapper userFavoriteMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @Override
    public List<UserFavoriteVo> list(Long userId) {
        List<UserFavorite> favorites = userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .orderByDesc(UserFavorite::getCreateTime));
        if (CollectionUtils.isEmpty(favorites)) {
            return Collections.emptyList();
        }
        List<Long> goodsIds = favorites.stream().map(UserFavorite::getGoodsId).collect(Collectors.toList());
        Map<Long, RentGoods> goodsMap = rentGoodsMapper.selectBatchIds(goodsIds).stream()
                .collect(Collectors.toMap(RentGoods::getId, Function.identity(), (a, b) -> a));
        return favorites.stream()
                .map(item -> toFavoriteVo(item, goodsMap.get(item.getGoodsId())))
                .filter(vo -> {
                    RentGoods goods = goodsMap.get(vo.getGoodsId());
                    return goods != null
                            && goods.getStatus() != null
                            && goods.getStatus() == 1
                            && merchantStoreOpenSupport.isMerchantStoreOpen(goods.getMerchantId());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserFavoriteToggleVo toggle(Long userId, Long goodsId) {
        RentGoods goods = rentGoodsMapper.selectById(goodsId);
        if (Objects.isNull(goods)) {
            throw new BizException("商品不存在");
        }
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getGoodsId, goods.getId())
                .last("LIMIT 1"));
        boolean favorite;
        if (existing != null) {
            userFavoriteMapper.purgeSoftDeleted(userId, goods.getId());
            userFavoriteMapper.deleteById(existing.getId());
            favorite = false;
        } else {
            assertCanFavorite(goods);
            if (userFavoriteMapper.restoreSoftDeleted(userId, goods.getId()) > 0) {
                favorite = true;
            } else {
                UserFavorite record = new UserFavorite();
                record.setUserId(userId);
                record.setGoodsId(goods.getId());
                userFavoriteMapper.insert(record);
                favorite = true;
            }
        }
        UserFavoriteToggleVo vo = new UserFavoriteToggleVo();
        vo.setGoodsId(goods.getId());
        vo.setFavorite(favorite);
        vo.setFavoriteCount(countByUser(userId));
        return vo;
    }

    @Override
    public boolean check(Long userId, Long goodsId) {
        RentGoods goods = rentGoodsMapper.selectById(goodsId);
        if (Objects.isNull(goods)) {
            throw new BizException("商品不存在");
        }
        UserFavorite existing = userFavoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getGoodsId, goodsId)
                .last("LIMIT 1"));
        return existing != null;
    }

    private int countByUser(Long userId) {
        return userFavoriteMapper.selectCount(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)).intValue();
    }

    private void assertCanFavorite(RentGoods goods) {
        if (goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品未上架");
        }
        merchantStoreOpenSupport.assertMerchantStoreOpen(goods.getMerchantId());
    }

    private UserFavoriteVo toFavoriteVo(UserFavorite favorite, RentGoods goods) {
        UserFavoriteVo vo = new UserFavoriteVo();
        vo.setFavoriteId(favorite.getId());
        vo.setGoodsId(favorite.getGoodsId());
        vo.setFavoriteTime(favorite.getCreateTime());
        if (goods != null) {
            vo.setGoodsNo(goods.getGoodsNo());
            vo.setGoodsName(goods.getName());
            vo.setCoverImage(goods.getCoverImage());
            vo.setRentPriceDay(goods.getRentPriceDay());
            vo.setDepositAmount(goods.getDepositAmount());
            vo.setGoodsStatus(goods.getStatus());
        }
        return vo;
    }
}
