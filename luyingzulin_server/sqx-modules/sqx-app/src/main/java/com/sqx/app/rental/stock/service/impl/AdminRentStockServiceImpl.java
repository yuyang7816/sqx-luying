package com.sqx.app.rental.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockAdjustBo;
import com.sqx.app.rental.stock.domain.bo.AdminRentStockPageBo;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.AdminRentStockListVo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.service.AdminRentStockService;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminRentStockServiceImpl implements AdminRentStockService {

    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentStockMapper rentStockMapper;
    @Resource
    private RentCategoryMapper rentCategoryMapper;
    @Resource
    private RentStockService rentStockService;

    @Override
    public IPage<AdminRentStockListVo> page(AdminRentStockPageBo pageBo) {
        Page<RentGoods> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<RentGoods> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(RentGoods::getId, id);
            } else {
                wrapper.like(RentGoods::getName, pageBo.getKeyword());
            }
        }
        if (pageBo.getCategoryId() != null) {
            wrapper.eq(RentGoods::getCategoryId, pageBo.getCategoryId());
        }
        wrapper.orderByDesc(RentGoods::getUpdateTime);

        IPage<RentGoods> goodsPage = rentGoodsMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(goodsPage.getRecords())) {
            return PageUtil.copyPage(goodsPage, Collections.emptyList());
        }

        List<Long> goodsIds = goodsPage.getRecords().stream().map(RentGoods::getId).collect(Collectors.toList());
        Map<Long, RentStock> stockMap = rentStockMapper.selectList(new LambdaQueryWrapper<RentStock>()
                        .in(RentStock::getGoodsId, goodsIds)
                        .eq(RentStock::getSpecId, 0L))
                .stream()
                .collect(Collectors.toMap(RentStock::getGoodsId, s -> s, (a, b) -> a));

        List<Long> categoryIds = goodsPage.getRecords().stream()
                .map(RentGoods::getCategoryId).filter(id -> id != null).distinct().collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : rentCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(RentCategory::getId, RentCategory::getName, (a, b) -> a));

        List<AdminRentStockListVo> records = goodsPage.getRecords().stream()
                .map(goods -> toListVo(goods, stockMap.get(goods.getId()), categoryNameMap.get(goods.getCategoryId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(goodsPage, records);
    }

    @Override
    public RentStockCheckVo occupyQuery(RentStockCheckBo checkBo) {
        return rentStockService.check(checkBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(AdminRentStockAdjustBo adjustBo) {
        if (adjustBo.getAdjustQty() == null || adjustBo.getAdjustQty() == 0) {
            throw new BizException("调整数量不能为0");
        }
        RentGoods goods = rentGoodsMapper.selectById(adjustBo.getGoodsId());
        if (goods == null) {
            throw new BizException("商品不存在");
        }
        Long specId = adjustBo.getSpecId() == null ? 0L : adjustBo.getSpecId();
        RentStock stock = rentStockMapper.selectOne(new LambdaQueryWrapper<RentStock>()
                .eq(RentStock::getGoodsId, adjustBo.getGoodsId())
                .eq(RentStock::getSpecId, specId)
                .last("LIMIT 1"));
        if (stock == null) {
            stock = new RentStock();
            stock.setGoodsId(adjustBo.getGoodsId());
            stock.setSpecId(specId);
            stock.setTotalQty(Math.max(adjustBo.getAdjustQty(), 0));
            stock.setAvailableQty(stock.getTotalQty());
            rentStockMapper.insert(stock);
            return;
        }
        int nextTotal = Math.max((stock.getTotalQty() == null ? 0 : stock.getTotalQty()) + adjustBo.getAdjustQty(), 0);
        int nextAvailable = Math.max((stock.getAvailableQty() == null ? 0 : stock.getAvailableQty()) + adjustBo.getAdjustQty(), 0);
        rentStockMapper.update(null, new LambdaUpdateWrapper<RentStock>()
                .eq(RentStock::getId, stock.getId())
                .set(RentStock::getTotalQty, nextTotal)
                .set(RentStock::getAvailableQty, nextAvailable));
    }

    private AdminRentStockListVo toListVo(RentGoods goods, RentStock stock, String categoryName) {
        AdminRentStockListVo vo = new AdminRentStockListVo();
        vo.setId(goods.getId());
        vo.setGoodsNo(goods.getGoodsNo());
        vo.setGoodsName(goods.getName());
        vo.setCategoryId(goods.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setStatus(goods.getStatus());
        int total = stock == null || stock.getTotalQty() == null ? 0 : stock.getTotalQty();
        int available = stock == null || stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
        vo.setTotalStock(total);
        vo.setAvailableStock(available);
        vo.setOccupiedStock(Math.max(total - available, 0));
        return vo;
    }
}
