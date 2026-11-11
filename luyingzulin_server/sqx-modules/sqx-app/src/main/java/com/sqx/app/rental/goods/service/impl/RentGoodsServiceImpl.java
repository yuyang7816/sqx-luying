package com.sqx.app.rental.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import com.sqx.app.rental.goods.domain.bo.RentGoodsInfoBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPackageItemBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSaveBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSpecItemBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateStateBo;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsImage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsPackageVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsParamVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsSpecVo;
import com.sqx.app.rental.goods.enums.RentGoodsSpecType;
import com.sqx.app.rental.goods.mapper.RentGoodsImageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsPackageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsSpecMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.app.rental.enums.RentType;
import com.sqx.app.rental.goods.service.RentGoodsService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租赁商品服务实现
 */
@Service
public class RentGoodsServiceImpl implements RentGoodsService {

    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentCategoryMapper rentCategoryMapper;
    @Resource
    private RentStockMapper rentStockMapper;
    @Resource
    private RentGoodsSpecMapper rentGoodsSpecMapper;
    @Resource
    private RentGoodsImageMapper rentGoodsImageMapper;
    @Resource
    private RentGoodsPackageMapper rentGoodsPackageMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @Override
    public IPage<RentGoodsListVo> page(RentGoodsPageBo pageBo) {
        Page<RentGoods> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());

        LambdaQueryWrapper<RentGoods> wrapper = new LambdaQueryWrapper<>();
        if (pageBo.getStatus() != null) {
            wrapper.eq(RentGoods::getStatus, pageBo.getStatus());
        }

        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(RentGoods::getId, id);
            } else {
                wrapper.and(w -> w.like(RentGoods::getName, pageBo.getKeyword())
                        .or()
                        .like(RentGoods::getSubtitle, pageBo.getKeyword()));
            }
        }

        if (pageBo.getCategoryId() != null) {
            List<Long> categoryIds = resolveCategoryIds(pageBo.getCategoryId());
            wrapper.in(RentGoods::getCategoryId, categoryIds);
        }

        if (pageBo.getMerchantId() != null) {
            wrapper.eq(RentGoods::getMerchantId, pageBo.getMerchantId());
        }

        applyRentTypeFilter(wrapper, pageBo.getRentType());
        applySort(wrapper, pageBo.getSortBy(), pageBo.getRentType());
        if (Boolean.TRUE.equals(pageBo.getOpenStoreOnly())) {
            merchantStoreOpenSupport.applyOpenStoreGoodsFilter(wrapper);
        }
        if (StringUtils.isNotBlank(pageBo.getCity())) {
            merchantStoreOpenSupport.applyCityGoodsFilter(wrapper, pageBo.getCity());
        }

        IPage<RentGoods> goodsPage = rentGoodsMapper.selectPage(page, wrapper);
        List<RentGoodsListVo> voList = buildListVo(goodsPage.getRecords(), pageBo.getRentType());
        return PageUtil.copyPage(goodsPage, voList);
    }

    @Override
    public RentGoodsDetailVo detail(Long id) {
        RentGoods goods = rentGoodsMapper.selectById(id);
        if (goods == null) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }

        RentGoodsDetailVo detailVo = new RentGoodsDetailVo();
        RentGoodsListVo baseVo = toListVo(goods, loadCategoryNameMap(Collections.singletonList(goods)),
                loadStockMap(Collections.singletonList(goods)),
                loadMerchantNameMap(Collections.singletonList(goods)), null);
        BeanUtils.copyProperties(baseVo, detailVo);
        detailVo.setDetail(goods.getDetail());

        List<RentGoodsImage> images = rentGoodsImageMapper.selectList(new LambdaQueryWrapper<RentGoodsImage>()
                .eq(RentGoodsImage::getGoodsId, id)
                .orderByAsc(RentGoodsImage::getSort));
        if (CollectionUtils.isNotEmpty(images)) {
            detailVo.setImages(images.stream().map(RentGoodsImage::getImageUrl).collect(Collectors.toList()));
        } else if (StringUtils.isNotBlank(goods.getCoverImage())) {
            detailVo.setImages(Collections.singletonList(goods.getCoverImage()));
        }

        List<RentGoodsSpec> specRows = rentGoodsSpecMapper.selectList(new LambdaQueryWrapper<RentGoodsSpec>()
                .eq(RentGoodsSpec::getGoodsId, id)
                .orderByAsc(RentGoodsSpec::getSort));
        List<RentGoodsSpecVo> skuSpecs = new ArrayList<>();
        List<RentGoodsParamVo> params = new ArrayList<>();
        for (RentGoodsSpec spec : specRows) {
            int specType = resolveSpecType(spec);
            if (specType == RentGoodsSpecType.PARAM) {
                RentGoodsParamVo paramVo = new RentGoodsParamVo();
                paramVo.setName(spec.getSpecName());
                paramVo.setValue(spec.getSpecValue());
                params.add(paramVo);
                continue;
            }
            RentGoodsSpecVo specVo = new RentGoodsSpecVo();
            specVo.setId(spec.getId());
            specVo.setName(spec.getSpecName());
            specVo.setPrice(parseSpecPrice(spec.getSpecValue()));
            skuSpecs.add(specVo);
        }
        detailVo.setSpecs(skuSpecs);
        detailVo.setParams(params);

        List<RentGoodsPackage> packages = rentGoodsPackageMapper.selectList(new LambdaQueryWrapper<RentGoodsPackage>()
                .eq(RentGoodsPackage::getGoodsId, id)
                .orderByAsc(RentGoodsPackage::getSort));
        detailVo.setPackages(packages.stream().map(pkg -> {
            RentGoodsPackageVo packageVo = new RentGoodsPackageVo();
            packageVo.setId(pkg.getId());
            packageVo.setPackageName(pkg.getPackageName());
            packageVo.setPackageDesc(pkg.getPackageDesc());
            packageVo.setPrice(pkg.getPrice());
            return packageVo;
        }).collect(Collectors.toList()));

        RentStock stock = rentStockMapper.selectOne(new LambdaQueryWrapper<RentStock>()
                .eq(RentStock::getGoodsId, id)
                .eq(RentStock::getSpecId, 0)
                .last("LIMIT 1"));
        if (stock != null) {
            detailVo.setTotalStock(stock.getTotalQty());
            detailVo.setAvailableStock(stock.getAvailableQty());
        }
        return detailVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInfo(RentGoodsSaveBo saveBo) {
        validateCategoryExists(saveBo.getCategoryId());

        RentGoods goods = new RentGoods();
        copyGoodsFields(saveBo, goods);
        if (goods.getStatus() == null) {
            goods.setStatus(0);
        }
        if (goods.getSort() == null) {
            goods.setSort(0);
        }
        if (StringUtils.isBlank(goods.getGoodsNo())) {
            goods.setGoodsNo(generateGoodsNo());
        }
        ensureGoodsNoUnique(goods.getGoodsNo(), null);

        rentGoodsMapper.insert(goods);
        saveRelatedData(goods.getId(), saveBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(RentGoodsUpdateBo updateBo) {
        RentGoods existing = rentGoodsMapper.selectById(updateBo.getId());
        if (existing == null) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }
        validateCategoryExists(updateBo.getCategoryId());

        RentGoods goods = new RentGoods();
        copyGoodsFields(updateBo, goods);
        goods.setId(updateBo.getId());
        if (StringUtils.isBlank(goods.getGoodsNo())) {
            goods.setGoodsNo(existing.getGoodsNo());
        }
        ensureGoodsNoUnique(goods.getGoodsNo(), updateBo.getId());

        rentGoodsMapper.updateById(goods);
        removeRelatedData(updateBo.getId(), false);
        saveRelatedData(updateBo.getId(), updateBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(RentGoodsInfoBo infoBo) {
        RentGoods existing = rentGoodsMapper.selectById(infoBo.getId());
        if (existing == null) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }
        rentGoodsMapper.deleteById(infoBo.getId());
        removeRelatedData(infoBo.getId(), true);
    }

    @Override
    public void updateState(RentGoodsUpdateStateBo updateStateBo) {
        RentGoods existing = rentGoodsMapper.selectById(updateStateBo.getId());
        if (existing == null) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }
        if (updateStateBo.getStatus() != 0 && updateStateBo.getStatus() != 1) {
            throw new BizException("状态仅支持 0下架 或 1上架");
        }
        LambdaUpdateWrapper<RentGoods> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(RentGoods::getId, updateStateBo.getId())
                .set(RentGoods::getStatus, updateStateBo.getStatus());
        rentGoodsMapper.update(null, updateWrapper);
    }

    private void copyGoodsFields(RentGoodsSaveBo saveBo, RentGoods goods) {
        BeanUtils.copyProperties(saveBo, goods);
        goods.setTags(joinTags(saveBo.getTags()));
        if (StringUtils.isBlank(goods.getOvernightStartTime())) {
            goods.setOvernightStartTime("18:00");
        }
        if (StringUtils.isBlank(goods.getOvernightEndTime())) {
            goods.setOvernightEndTime("10:00");
        }
    }

    private void validateCategoryExists(Long categoryId) {
        RentCategory category = rentCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BizException("分类不存在");
        }
    }

    private void ensureGoodsNoUnique(String goodsNo, Long excludeId) {
        if (StringUtils.isBlank(goodsNo)) {
            return;
        }
        LambdaQueryWrapper<RentGoods> wrapper = new LambdaQueryWrapper<RentGoods>()
                .eq(RentGoods::getGoodsNo, goodsNo);
        if (excludeId != null) {
            wrapper.ne(RentGoods::getId, excludeId);
        }
        Long count = rentGoodsMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BizException("商品编号已存在");
        }
    }

    private String generateGoodsNo() {
        return "RG" + System.currentTimeMillis();
    }

    private String joinTags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            return null;
        }
        return tags.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    private void saveRelatedData(Long goodsId, RentGoodsSaveBo saveBo) {
        saveSpecRows(goodsId, saveBo.getSpecs(), RentGoodsSpecType.SKU);
        saveSpecRows(goodsId, saveBo.getParams(), RentGoodsSpecType.PARAM);
        savePackages(goodsId, saveBo.getPackages());
        saveImages(goodsId, saveBo.getImages(), saveBo.getCoverImage());
        saveStock(goodsId, saveBo.getTotalStock());
    }

    private void removeRelatedData(Long goodsId, boolean removeStock) {
        rentGoodsSpecMapper.delete(new LambdaQueryWrapper<RentGoodsSpec>().eq(RentGoodsSpec::getGoodsId, goodsId));
        rentGoodsPackageMapper.delete(new LambdaQueryWrapper<RentGoodsPackage>().eq(RentGoodsPackage::getGoodsId, goodsId));
        rentGoodsImageMapper.delete(new LambdaQueryWrapper<RentGoodsImage>().eq(RentGoodsImage::getGoodsId, goodsId));
        if (removeStock) {
            rentStockMapper.delete(new LambdaQueryWrapper<RentStock>().eq(RentStock::getGoodsId, goodsId));
        }
    }

    private void saveSpecRows(Long goodsId, List<RentGoodsSpecItemBo> rows, int specType) {
        if (CollectionUtils.isEmpty(rows)) {
            return;
        }
        int sort = 1;
        for (RentGoodsSpecItemBo item : rows) {
            if (StringUtils.isBlank(item.getSpecName())) {
                continue;
            }
            String storedValue = resolveSpecStoredValue(item, specType);
            if (StringUtils.isBlank(storedValue)) {
                continue;
            }
            RentGoodsSpec spec = new RentGoodsSpec();
            spec.setGoodsId(goodsId);
            spec.setSpecName(item.getSpecName().trim());
            spec.setSpecValue(storedValue);
            spec.setSpecType(item.getSpecType() != null ? item.getSpecType() : specType);
            spec.setSort(item.getSort() != null ? item.getSort() : sort++);
            rentGoodsSpecMapper.insert(spec);
        }
    }

    private String resolveSpecStoredValue(RentGoodsSpecItemBo item, int specType) {
        if (specType == RentGoodsSpecType.SKU) {
            if (item.getSpecPrice() != null) {
                return item.getSpecPrice().stripTrailingZeros().toPlainString();
            }
            return StringUtils.defaultString(item.getSpecValue(), "").trim();
        }
        return StringUtils.defaultString(item.getSpecValue(), "").trim();
    }

    private int resolveSpecType(RentGoodsSpec spec) {
        if (spec.getSpecType() != null) {
            return spec.getSpecType();
        }
        return parseSpecPrice(spec.getSpecValue()) != null
                ? RentGoodsSpecType.SKU
                : RentGoodsSpecType.PARAM;
    }

    private BigDecimal parseSpecPrice(String specValue) {
        if (StringUtils.isBlank(specValue)) {
            return null;
        }
        try {
            return new BigDecimal(specValue.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void savePackages(Long goodsId, List<RentGoodsPackageItemBo> packages) {
        if (CollectionUtils.isEmpty(packages)) {
            return;
        }
        int sort = 1;
        for (RentGoodsPackageItemBo item : packages) {
            RentGoodsPackage pkg = new RentGoodsPackage();
            pkg.setGoodsId(goodsId);
            pkg.setPackageName(item.getPackageName());
            pkg.setPackageDesc(item.getPackageDesc());
            pkg.setPrice(item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO);
            pkg.setSort(item.getSort() != null ? item.getSort() : sort++);
            rentGoodsPackageMapper.insert(pkg);
        }
    }

    private void saveImages(Long goodsId, List<String> images, String coverImage) {
        List<String> imageUrls = CollectionUtils.isNotEmpty(images) ? images : Collections.emptyList();
        if (CollectionUtils.isEmpty(imageUrls) && StringUtils.isNotBlank(coverImage)) {
            imageUrls = Collections.singletonList(coverImage);
        }
        if (CollectionUtils.isEmpty(imageUrls)) {
            return;
        }
        int sort = 1;
        for (String imageUrl : imageUrls) {
            if (StringUtils.isBlank(imageUrl)) {
                continue;
            }
            RentGoodsImage image = new RentGoodsImage();
            image.setGoodsId(goodsId);
            image.setImageUrl(imageUrl.trim());
            image.setSort(sort++);
            rentGoodsImageMapper.insert(image);
        }
    }

    private void saveStock(Long goodsId, Integer totalStock) {
        if (totalStock == null) {
            return;
        }
        RentStock stock = rentStockMapper.selectOne(new LambdaQueryWrapper<RentStock>()
                .eq(RentStock::getGoodsId, goodsId)
                .eq(RentStock::getSpecId, 0L)
                .last("LIMIT 1"));
        if (stock != null) {
            int currentTotal = stock.getTotalQty() == null ? 0 : stock.getTotalQty();
            int currentAvailable = stock.getAvailableQty() == null ? 0 : stock.getAvailableQty();
            int rented = Math.max(currentTotal - currentAvailable, 0);
            int newAvailable = Math.max(totalStock - rented, 0);
            rentStockMapper.update(null, new LambdaUpdateWrapper<RentStock>()
                    .eq(RentStock::getId, stock.getId())
                    .set(RentStock::getTotalQty, totalStock)
                    .set(RentStock::getAvailableQty, newAvailable));
            return;
        }
        RentStock newStock = new RentStock();
        newStock.setGoodsId(goodsId);
        newStock.setSpecId(0L);
        newStock.setTotalQty(totalStock);
        newStock.setAvailableQty(totalStock);
        rentStockMapper.insert(newStock);
    }

    private List<RentGoodsListVo> buildListVo(List<RentGoods> records, String rentType) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNameMap = loadCategoryNameMap(records);
        Map<Long, RentStock> stockMap = loadStockMap(records);
        Map<Long, String> merchantNameMap = loadMerchantNameMap(records);
        List<RentGoodsListVo> voList = new ArrayList<>();
        for (RentGoods goods : records) {
            voList.add(toListVo(goods, categoryNameMap, stockMap, merchantNameMap, rentType));
        }
        return voList;
    }

    private RentGoodsListVo toListVo(RentGoods goods, Map<Long, String> categoryNameMap,
                                     Map<Long, RentStock> stockMap, Map<Long, String> merchantNameMap,
                                     String rentType) {
        RentGoodsListVo vo = new RentGoodsListVo();
        BeanUtils.copyProperties(goods, vo);
        vo.setCategoryName(categoryNameMap.get(goods.getCategoryId()));
        vo.setMerchantName(merchantNameMap.get(goods.getMerchantId()));
        vo.setRentPrice(resolveDisplayPrice(goods, rentType));
        RentStock stock = stockMap.get(goods.getId());
        if (stock != null) {
            vo.setAvailableStock(stock.getAvailableQty());
        }
        if (StringUtils.isNotBlank(goods.getTags())) {
            vo.setTags(Arrays.stream(goods.getTags().split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private Map<Long, String> loadCategoryNameMap(List<RentGoods> records) {
        List<Long> categoryIds = records.stream()
                .map(RentGoods::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(categoryIds)) {
            return Collections.emptyMap();
        }
        List<RentCategory> categories = rentCategoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> categoryNameMap = new HashMap<>();
        for (RentCategory category : categories) {
            if (category == null || category.getId() == null) {
                continue;
            }
            categoryNameMap.put(category.getId(), StringUtils.defaultString(category.getName()));
        }
        return categoryNameMap;
    }

    private Map<Long, String> loadMerchantNameMap(List<RentGoods> records) {
        List<Long> merchantIds = records.stream()
                .map(RentGoods::getMerchantId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(merchantIds)) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = merchantMapper.selectBatchIds(merchantIds);
        Map<Long, String> merchantNameMap = new HashMap<>();
        for (Merchant merchant : merchants) {
            if (merchant == null || merchant.getId() == null) {
                continue;
            }
            merchantNameMap.put(merchant.getId(), StringUtils.defaultString(merchant.getName()));
        }
        return merchantNameMap;
    }

    private Map<Long, RentStock> loadStockMap(List<RentGoods> records) {
        List<Long> goodsIds = records.stream().map(RentGoods::getId).collect(Collectors.toList());
        List<RentStock> stocks = rentStockMapper.selectList(new LambdaQueryWrapper<RentStock>()
                .in(RentStock::getGoodsId, goodsIds)
                .eq(RentStock::getSpecId, 0));
        return stocks.stream().collect(Collectors.toMap(RentStock::getGoodsId, item -> item, (a, b) -> a));
    }

    private List<Long> resolveCategoryIds(Long categoryId) {
        RentCategory category = rentCategoryMapper.selectById(categoryId);
        if (category == null) {
            return Collections.singletonList(categoryId);
        }
        if (category.getParentId() == null || category.getParentId() == 0) {
            List<RentCategory> children = rentCategoryMapper.selectList(new LambdaQueryWrapper<RentCategory>()
                    .eq(RentCategory::getParentId, categoryId)
                    .eq(RentCategory::getStatus, 1));
            if (CollectionUtils.isEmpty(children)) {
                return Collections.singletonList(categoryId);
            }
            return children.stream().map(RentCategory::getId).collect(Collectors.toList());
        }
        return Collections.singletonList(categoryId);
    }

    private void applyRentTypeFilter(LambdaQueryWrapper<RentGoods> wrapper, String rentTypeCode) {
        if (StringUtils.isBlank(rentTypeCode)) {
            return;
        }
        RentType rentType = RentType.fromCode(rentTypeCode);
        if (rentType == RentType.HOUR) {
            wrapper.isNotNull(RentGoods::getRentPriceHour)
                    .gt(RentGoods::getRentPriceHour, BigDecimal.ZERO);
            return;
        }
        if (rentType == RentType.OVERNIGHT) {
            wrapper.isNotNull(RentGoods::getRentPriceOvernight)
                    .gt(RentGoods::getRentPriceOvernight, BigDecimal.ZERO);
            return;
        }
        wrapper.gt(RentGoods::getRentPriceDay, BigDecimal.ZERO);
    }

    private BigDecimal resolveDisplayPrice(RentGoods goods, String rentTypeCode) {
        if (StringUtils.isNotBlank(rentTypeCode)) {
            RentType rentType = RentType.fromCode(rentTypeCode);
            if (rentType == RentType.HOUR && goods.getRentPriceHour() != null) {
                return goods.getRentPriceHour();
            }
            if (rentType == RentType.OVERNIGHT && goods.getRentPriceOvernight() != null) {
                return goods.getRentPriceOvernight();
            }
        }
        return goods.getRentPriceDay();
    }

    private void applySort(LambdaQueryWrapper<RentGoods> wrapper, String sortBy, String rentTypeCode) {
        if ("price".equals(sortBy)) {
            if (RentType.HOUR.getCode().equals(rentTypeCode)) {
                wrapper.orderByAsc(RentGoods::getRentPriceHour);
                return;
            }
            if (RentType.OVERNIGHT.getCode().equals(rentTypeCode)) {
                wrapper.orderByAsc(RentGoods::getRentPriceOvernight);
                return;
            }
            wrapper.orderByAsc(RentGoods::getRentPriceDay);
            return;
        }
        if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(RentGoods::getSalesCount);
            return;
        }
        wrapper.orderByAsc(RentGoods::getSort).orderByDesc(RentGoods::getId);
    }
}
