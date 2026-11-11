package com.sqx.app.merchant.goods.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsPageBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsShelfBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantStockAdjustBo;
import com.sqx.app.merchant.goods.service.MerchantGoodsService;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.goods.domain.bo.RentGoodsInfoBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSaveBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateStateBo;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.app.rental.goods.service.RentGoodsService;
import com.sqx.common.core.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import javax.annotation.Resource;

@Service
public class MerchantGoodsServiceImpl implements MerchantGoodsService {

    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private RentGoodsService rentGoodsService;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentStockMapper rentStockMapper;

    @Override
    public IPage<RentGoodsListVo> page(Long userId, MerchantGoodsPageBo pageBo) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        RentGoodsPageBo queryBo = new RentGoodsPageBo();
        queryBo.setPage(pageBo.getPage());
        queryBo.setLimit(pageBo.getLimit());
        queryBo.setKeyword(pageBo.getKeyword());
        queryBo.setCategoryId(pageBo.getCategoryId());
        queryBo.setStatus(pageBo.getStatus());
        queryBo.setMerchantId(ctx.getMerchantId());
        return rentGoodsService.page(queryBo);
    }

    @Override
    public RentGoodsDetailVo detail(Long userId, Long goodsId) {
        requireOwnedGoods(userId, goodsId);
        return rentGoodsService.detail(goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long userId, RentGoodsSaveBo saveBo) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        saveBo.setMerchantId(ctx.getMerchantId());
        if (saveBo.getStatus() == null) {
            saveBo.setStatus(1);
        }
        rentGoodsService.saveInfo(saveBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, RentGoodsUpdateBo updateBo) {
        if (updateBo.getId() == null) {
            throw new BizException("商品ID不能为空");
        }
        requireOwnedGoods(userId, updateBo.getId());
        rentGoodsService.updateInfo(updateBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shelf(Long userId, MerchantGoodsShelfBo shelfBo) {
        requireOwnedGoods(userId, shelfBo.getGoodsId());
        RentGoodsUpdateStateBo stateBo = new RentGoodsUpdateStateBo();
        stateBo.setId(shelfBo.getGoodsId());
        stateBo.setStatus(shelfBo.getStatus());
        rentGoodsService.updateState(stateBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(Long userId, MerchantStockAdjustBo adjustBo) {
        requireOwnedGoods(userId, adjustBo.getGoodsId());
        Long specId = adjustBo.getSpecId() == null ? 0L : adjustBo.getSpecId();
        RentStock stock = rentStockMapper.selectOne(new LambdaQueryWrapper<RentStock>()
                .eq(RentStock::getGoodsId, adjustBo.getGoodsId())
                .eq(RentStock::getSpecId, specId)
                .last("LIMIT 1"));
        if (stock == null) {
            stock = new RentStock();
            stock.setGoodsId(adjustBo.getGoodsId());
            stock.setSpecId(specId);
            stock.setTotalQty(0);
            stock.setAvailableQty(0);
            rentStockMapper.insert(stock);
        }

        int delta = "out".equalsIgnoreCase(adjustBo.getAdjustType())
                ? -adjustBo.getQuantity() : adjustBo.getQuantity();
        int newTotal = Math.max((stock.getTotalQty() == null ? 0 : stock.getTotalQty()) + delta, 0);
        int newAvailable = Math.max((stock.getAvailableQty() == null ? 0 : stock.getAvailableQty()) + delta, 0);

        rentStockMapper.update(null, new LambdaUpdateWrapper<RentStock>()
                .eq(RentStock::getId, stock.getId())
                .set(RentStock::getTotalQty, newTotal)
                .set(RentStock::getAvailableQty, newAvailable));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long goodsId) {
        requireOwnedGoods(userId, goodsId);
        RentGoodsInfoBo infoBo = new RentGoodsInfoBo();
        infoBo.setId(goodsId);
        rentGoodsService.delete(infoBo);
    }

    private RentGoods requireOwnedGoods(Long userId, Long goodsId) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContextForOperation(userId);
        RentGoods goods = rentGoodsMapper.selectById(goodsId);
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        if (!ctx.getMerchantId().equals(goods.getMerchantId())) {
            throw new BizException("无权操作该商品");
        }
        return goods;
    }
}
