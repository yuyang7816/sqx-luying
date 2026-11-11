package com.sqx.app.rental.goods.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsImage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import com.sqx.app.rental.goods.domain.entity.RentStock;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;
import com.sqx.app.rental.goods.mapper.RentGoodsImageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsPackageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsSpecMapper;
import com.sqx.app.rental.goods.mapper.RentStockMapper;
import com.sqx.common.core.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M01/M02：商品分页筛选与详情 VO 组装单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentGoodsServiceImplTest {

    @Mock
    private RentGoodsMapper rentGoodsMapper;
    @Mock
    private RentCategoryMapper rentCategoryMapper;
    @Mock
    private RentStockMapper rentStockMapper;
    @Mock
    private RentGoodsSpecMapper rentGoodsSpecMapper;
    @Mock
    private RentGoodsImageMapper rentGoodsImageMapper;
    @Mock
    private RentGoodsPackageMapper rentGoodsPackageMapper;
    @Mock
    private MerchantMapper merchantMapper;
    @Mock
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @InjectMocks
    private RentGoodsServiceImpl rentGoodsService;

    @Test
    void pageShouldApplyOpenStoreFilterWhenRequested() {
        RentGoods goods = sampleGoods();
        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setOpenStoreOnly(true);

        rentGoodsService.page(pageBo);

        verify(merchantStoreOpenSupport).applyOpenStoreGoodsFilter(any());
    }

    @Test
    void pageShouldApplyCityFilterWhenCityProvided() {
        RentGoods goods = sampleGoods();
        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setCity("西安");

        rentGoodsService.page(pageBo);

        verify(merchantStoreOpenSupport).applyCityGoodsFilter(any(), org.mockito.ArgumentMatchers.eq("西安"));
    }

    @Test
    void pageShouldSetDisplayPriceForHourRentType() {
        RentGoods goods = sampleGoods();
        goods.setRentPriceHour(new BigDecimal("4.90"));

        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setRentType("hour");

        IPage<RentGoodsListVo> result = rentGoodsService.page(pageBo);

        assertEquals(1, result.getRecords().size());
        assertEquals(new BigDecimal("4.90"), result.getRecords().get(0).getRentPrice());
        verify(rentGoodsMapper).selectPage(any(), any());
    }

    @Test
    void pageShouldSetDisplayPriceForOvernightRentType() {
        RentGoods goods = sampleGoods();
        goods.setRentPriceOvernight(new BigDecimal("39.90"));

        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setRentType("overnight");

        IPage<RentGoodsListVo> result = rentGoodsService.page(pageBo);

        assertEquals(new BigDecimal("39.90"), result.getRecords().get(0).getRentPrice());
    }

    @Test
    void pageShouldDefaultDisplayPriceToDayRentType() {
        RentGoods goods = sampleGoods();
        goods.setRentPriceDay(new BigDecimal("29.90"));

        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setRentType("day");

        IPage<RentGoodsListVo> result = rentGoodsService.page(pageBo);

        assertEquals(new BigDecimal("29.90"), result.getRecords().get(0).getRentPrice());
    }

    @Test
    void pageShouldEnrichCategoryNameAndTags() {
        RentGoods goods = sampleGoods();
        goods.setTags("新品,免押金");

        mockPageResult(goods);
        RentCategory category = new RentCategory();
        category.setId(11L);
        category.setName("帐篷");
        when(rentCategoryMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(category));

        IPage<RentGoodsListVo> result = rentGoodsService.page(basePageBo());

        RentGoodsListVo vo = result.getRecords().get(0);
        assertEquals("帐篷", vo.getCategoryName());
        assertEquals(Arrays.asList("新品", "免押金"), vo.getTags());
    }

    @Test
    void pageShouldTolerateNullMerchantName() {
        RentGoods goods = sampleGoods();
        goods.setMerchantId(9L);

        mockPageResult(goods);
        Merchant merchant = new Merchant();
        merchant.setId(9L);
        merchant.setName(null);
        when(merchantMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(merchant));

        IPage<RentGoodsListVo> result = rentGoodsService.page(basePageBo());

        assertEquals("", result.getRecords().get(0).getMerchantName());
    }

    @Test
    void pageShouldEnrichAvailableStock() {
        RentGoods goods = sampleGoods();
        mockPageResult(goods);

        RentStock stock = new RentStock();
        stock.setGoodsId(1L);
        stock.setSpecId(0L);
        stock.setAvailableQty(12);
        when(rentStockMapper.selectList(any())).thenReturn(Collections.singletonList(stock));

        IPage<RentGoodsListVo> result = rentGoodsService.page(basePageBo());

        assertEquals(12, result.getRecords().get(0).getAvailableStock());
    }

    @Test
    void pageShouldInvokeMapperWhenKeywordFilterProvided() {
        RentGoods goods = sampleGoods();
        mockPageResult(goods);

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setKeyword("帐篷");

        IPage<RentGoodsListVo> result = rentGoodsService.page(pageBo);

        assertEquals(1, result.getRecords().size());
        verify(rentGoodsMapper).selectPage(any(), any());
    }

    @Test
    void pageShouldResolveParentCategoryToChildrenIds() {
        RentGoods goods = sampleGoods();
        mockPageResult(goods);

        RentCategory parent = new RentCategory();
        parent.setId(10L);
        parent.setParentId(0L);
        when(rentCategoryMapper.selectById(10L)).thenReturn(parent);

        RentCategory child = new RentCategory();
        child.setId(11L);
        child.setParentId(10L);
        child.setStatus(1);
        when(rentCategoryMapper.selectList(any())).thenReturn(Collections.singletonList(child));

        RentGoodsPageBo pageBo = basePageBo();
        pageBo.setCategoryId(10L);

        IPage<RentGoodsListVo> result = rentGoodsService.page(pageBo);

        assertEquals(1, result.getRecords().size());
        verify(rentCategoryMapper).selectById(10L);
    }

    @Test
    void detailShouldAssembleImagesSpecsPackagesAndStock() {
        RentGoods goods = sampleGoods();
        goods.setDetail("详情描述");
        goods.setCoverImage("https://example.com/cover.jpg");
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);

        RentCategory category = new RentCategory();
        category.setId(11L);
        category.setName("帐篷");
        when(rentCategoryMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(category));

        RentStock stock = new RentStock();
        stock.setGoodsId(1L);
        stock.setSpecId(0L);
        stock.setTotalQty(20);
        stock.setAvailableQty(12);
        when(rentStockMapper.selectList(any())).thenReturn(Collections.singletonList(stock));
        when(rentStockMapper.selectOne(any())).thenReturn(stock);

        RentGoodsImage image = new RentGoodsImage();
        image.setImageUrl("https://example.com/1.jpg");
        when(rentGoodsImageMapper.selectList(any())).thenReturn(Collections.singletonList(image));

        RentGoodsSpec spec = new RentGoodsSpec();
        spec.setSpecName("颜色");
        spec.setSpecValue("军绿");
        spec.setSpecType(2);
        when(rentGoodsSpecMapper.selectList(any())).thenReturn(Collections.singletonList(spec));

        RentGoodsPackage pkg = new RentGoodsPackage();
        pkg.setId(100L);
        pkg.setPackageName("防潮垫");
        pkg.setPackageDesc("加厚款");
        pkg.setPrice(new BigDecimal("5.00"));
        when(rentGoodsPackageMapper.selectList(any())).thenReturn(Collections.singletonList(pkg));

        RentGoodsDetailVo detailVo = rentGoodsService.detail(1L);

        assertEquals("详情描述", detailVo.getDetail());
        assertEquals("帐篷", detailVo.getCategoryName());
        assertEquals(Collections.singletonList("https://example.com/1.jpg"), detailVo.getImages());
        assertTrue(detailVo.getSpecs() == null || detailVo.getSpecs().isEmpty());
        assertEquals(1, detailVo.getParams().size());
        assertEquals("颜色", detailVo.getParams().get(0).getName());
        assertEquals("军绿", detailVo.getParams().get(0).getValue());
        assertEquals(1, detailVo.getPackages().size());
        assertEquals("防潮垫", detailVo.getPackages().get(0).getPackageName());
        assertEquals(new BigDecimal("5.00"), detailVo.getPackages().get(0).getPrice());
        assertEquals(20, detailVo.getTotalStock());
        assertEquals(12, detailVo.getAvailableStock());
    }

    @Test
    void detailShouldFallbackToCoverImageWhenNoGalleryImages() {
        RentGoods goods = sampleGoods();
        goods.setCoverImage("https://example.com/cover.jpg");
        when(rentGoodsMapper.selectById(1L)).thenReturn(goods);
        when(rentCategoryMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(rentStockMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(rentStockMapper.selectOne(any())).thenReturn(null);
        when(rentGoodsImageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(rentGoodsSpecMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(rentGoodsPackageMapper.selectList(any())).thenReturn(Collections.emptyList());

        RentGoodsDetailVo detailVo = rentGoodsService.detail(1L);

        assertEquals(Collections.singletonList("https://example.com/cover.jpg"), detailVo.getImages());
    }

    @Test
    void detailShouldThrowWhenGoodsNotFound() {
        when(rentGoodsMapper.selectById(999L)).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> rentGoodsService.detail(999L));

        assertEquals(AppError.INFO_NOT_FOUND, ex.getErrorCode());
    }

    private void mockPageResult(RentGoods goods) {
        Page<RentGoods> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(goods));
        page.setTotal(1);
        when(rentGoodsMapper.selectPage(any(), any())).thenReturn(page);
        when(rentCategoryMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(rentStockMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    private static RentGoodsPageBo basePageBo() {
        RentGoodsPageBo pageBo = new RentGoodsPageBo();
        pageBo.setPage(1L);
        pageBo.setLimit(10L);
        pageBo.setStatus(1);
        return pageBo;
    }

    private static RentGoods sampleGoods() {
        RentGoods goods = new RentGoods();
        goods.setId(1L);
        goods.setCategoryId(11L);
        goods.setName("测试帐篷");
        goods.setRentPriceDay(new BigDecimal("29.90"));
        goods.setDepositAmount(new BigDecimal("200.00"));
        goods.setSalesCount(10);
        goods.setStatus(1);
        return goods;
    }
}
