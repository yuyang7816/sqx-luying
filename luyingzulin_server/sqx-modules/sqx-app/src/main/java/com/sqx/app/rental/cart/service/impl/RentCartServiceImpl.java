package com.sqx.app.rental.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sqx.app.merchant.support.MerchantStoreOpenSupport;
import com.sqx.app.rental.cart.domain.bo.*;
import com.sqx.app.rental.cart.domain.entity.RentCart;
import com.sqx.app.rental.cart.domain.vo.RentCartCheckoutPreviewVo;
import com.sqx.app.rental.cart.domain.vo.RentCartItemVo;
import com.sqx.app.rental.cart.mapper.RentCartMapper;
import com.sqx.app.rental.cart.service.RentCartService;
import com.sqx.app.rental.enums.PickupType;
import com.sqx.app.rental.enums.RentType;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.domain.entity.RentGoodsPackage;
import com.sqx.app.rental.goods.domain.entity.RentGoodsSpec;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsPackageMapper;
import com.sqx.app.rental.goods.mapper.RentGoodsSpecMapper;
import com.sqx.app.rental.order.support.RentOrderPriceCalculator;
import com.sqx.app.rental.order.support.RentalDeliveryFeeSupport;
import com.sqx.app.rental.stock.domain.bo.RentStockCheckBo;
import com.sqx.app.rental.stock.domain.vo.RentStockCheckVo;
import com.sqx.app.rental.stock.service.RentStockService;
import com.sqx.app.rental.user.service.UserCouponService;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 租赁购物车服务实现
 */
@Service
public class RentCartServiceImpl implements RentCartService {

    @Resource
    private RentCartMapper rentCartMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private RentGoodsSpecMapper rentGoodsSpecMapper;
    @Resource
    private RentGoodsPackageMapper rentGoodsPackageMapper;
    @Resource
    private RentStockService rentStockService;
    @Resource
    private UserCouponService userCouponService;
    @Resource
    private RentalDeliveryFeeSupport rentalDeliveryFeeSupport;
    @Resource
    private MerchantStoreOpenSupport merchantStoreOpenSupport;

    @Override
    public List<RentCartItemVo> list(Long userId) {
        List<RentCart> carts = rentCartMapper.selectList(new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getUserId, userId)
                .orderByDesc(RentCart::getUpdateTime));
        if (CollectionUtils.isEmpty(carts)) {
            return Collections.emptyList();
        }
        return buildItemVos(carts);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, RentCartAddBo addBo) {
        validateRentPeriod(addBo.getStartTime(), addBo.getEndTime());
        RentType.fromCode(addBo.getRentType());

        Long specId = addBo.getSpecId() == null ? 0L : addBo.getSpecId();
        Long packageId = addBo.getPackageId() == null ? 0L : addBo.getPackageId();
        RentCart existing = rentCartMapper.selectOne(new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getUserId, userId)
                .eq(RentCart::getGoodsId, addBo.getGoodsId())
                .eq(RentCart::getSpecId, specId)
                .eq(RentCart::getPackageId, packageId)
                .eq(RentCart::getRentType, addBo.getRentType())
                .eq(RentCart::getStartTime, addBo.getStartTime())
                .eq(RentCart::getEndTime, addBo.getEndTime())
                .last("LIMIT 1"));
        if (existing != null) {
            int newQty = (existing.getQty() == null ? 0 : existing.getQty())
                    + (addBo.getQty() == null ? 1 : addBo.getQty());
            existing.setQty(newQty);
            existing.setSelected(1);
            rentCartMapper.updateById(existing);
            return existing.getId();
        }

        requireOnShelfGoods(addBo.getGoodsId());
        RentCart cart = new RentCart();
        cart.setUserId(userId);
        cart.setGoodsId(addBo.getGoodsId());
        cart.setSpecId(specId);
        cart.setPackageId(packageId);
        cart.setRentType(addBo.getRentType());
        cart.setStartTime(addBo.getStartTime());
        cart.setEndTime(addBo.getEndTime());
        cart.setQty(addBo.getQty() == null ? 1 : addBo.getQty());
        cart.setSelected(1);
        rentCartMapper.insert(cart);
        return cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, RentCartUpdateBo updateBo) {
        RentCart cart = requireOwnedCart(userId, updateBo.getId());
        if (updateBo.getSpecId() != null) {
            cart.setSpecId(updateBo.getSpecId());
        }
        if (updateBo.getPackageId() != null) {
            cart.setPackageId(updateBo.getPackageId());
        }
        if (StringUtils.isNotBlank(updateBo.getRentType())) {
            RentType.fromCode(updateBo.getRentType());
            cart.setRentType(updateBo.getRentType());
        }
        if (updateBo.getStartTime() != null) {
            cart.setStartTime(updateBo.getStartTime());
        }
        if (updateBo.getEndTime() != null) {
            cart.setEndTime(updateBo.getEndTime());
        }
        if (updateBo.getQty() != null) {
            cart.setQty(updateBo.getQty());
        }
        validateRentPeriod(cart.getStartTime(), cart.getEndTime());
        rentCartMapper.updateById(cart);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        requireOwnedCart(userId, id);
        rentCartMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long userId, RentCartDeleteBo deleteBo) {
        if (CollectionUtils.isEmpty(deleteBo.getIds())) {
            return;
        }
        rentCartMapper.delete(new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getUserId, userId)
                .in(RentCart::getId, deleteBo.getIds()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void select(Long userId, RentCartSelectBo selectBo) {
        int selected = Boolean.TRUE.equals(selectBo.getSelected()) ? 1 : 0;
        if (Boolean.TRUE.equals(selectBo.getSelectAll())) {
            rentCartMapper.update(null, new LambdaUpdateWrapper<RentCart>()
                    .eq(RentCart::getUserId, userId)
                    .set(RentCart::getSelected, selected));
            return;
        }
        if (CollectionUtils.isNotEmpty(selectBo.getIds())) {
            rentCartMapper.update(null, new LambdaUpdateWrapper<RentCart>()
                    .eq(RentCart::getUserId, userId)
                    .in(RentCart::getId, selectBo.getIds())
                    .set(RentCart::getSelected, selected));
            return;
        }
        if (selectBo.getId() != null) {
            requireOwnedCart(userId, selectBo.getId());
            rentCartMapper.update(null, new LambdaUpdateWrapper<RentCart>()
                    .eq(RentCart::getId, selectBo.getId())
                    .eq(RentCart::getUserId, userId)
                    .set(RentCart::getSelected, selected));
        }
    }

    @Override
    public RentCartCheckoutPreviewVo checkoutPreview(Long userId, RentCartCheckoutPreviewBo previewBo) {
        PickupType.fromCode(previewBo.getPickupType());
        List<RentCart> carts = loadCheckoutCarts(userId, previewBo.getCartIds());
        if (CollectionUtils.isEmpty(carts)) {
            throw new BizException("请选择要结算的商品");
        }

        BigDecimal rentAmount = BigDecimal.ZERO;
        BigDecimal depositAmount = BigDecimal.ZERO;
        List<RentCartCheckoutPreviewVo.RentCartCheckoutItemVo> itemVos = new ArrayList<>();
        List<RentCartCheckoutPreviewVo.ConfirmItem> confirmItems = new ArrayList<>();
        List<Long> cartIds = new ArrayList<>();

        for (RentCart cart : carts) {
            RentCartItemVo itemVo = buildSingleItemVo(cart);
            if (Boolean.TRUE.equals(itemVo.getInvalid())) {
                throw new BizException("存在失效商品：" + itemVo.getInvalidReason());
            }
            rentAmount = rentAmount.add(itemVo.getItemRentAmount());
            depositAmount = depositAmount.add(itemVo.getItemDepositAmount());
            cartIds.add(cart.getId());
            Long cartPackageId = normalizePackageId(cart.getPackageId());

            RentCartCheckoutPreviewVo.RentCartCheckoutItemVo checkoutItem =
                    new RentCartCheckoutPreviewVo.RentCartCheckoutItemVo();
            checkoutItem.setCartId(cart.getId());
            checkoutItem.setGoodsId(cart.getGoodsId());
            checkoutItem.setGoodsName(itemVo.getGoodsName());
            checkoutItem.setSpecId(cart.getSpecId());
            checkoutItem.setPackageId(cartPackageId > 0 ? cartPackageId : null);
            checkoutItem.setRentType(cart.getRentType());
            checkoutItem.setStartTime(cart.getStartTime());
            checkoutItem.setEndTime(cart.getEndTime());
            checkoutItem.setDays(RentOrderPriceCalculator.resolveDays(cart.getStartTime(), cart.getEndTime(), null));
            checkoutItem.setQuantity(cart.getQty());
            checkoutItem.setItemRentAmount(itemVo.getItemRentAmount());
            checkoutItem.setItemDepositAmount(itemVo.getItemDepositAmount());
            itemVos.add(checkoutItem);

            RentCartCheckoutPreviewVo.ConfirmItem confirmItem = new RentCartCheckoutPreviewVo.ConfirmItem();
            confirmItem.setGoodsId(cart.getGoodsId());
            confirmItem.setSpecId(cart.getSpecId());
            confirmItem.setPackageId(cartPackageId > 0 ? cartPackageId : null);
            confirmItem.setRentType(cart.getRentType());
            confirmItem.setStartTime(cart.getStartTime());
            confirmItem.setEndTime(cart.getEndTime());
            confirmItem.setDays(checkoutItem.getDays());
            confirmItem.setQuantity(cart.getQty());
            confirmItems.add(confirmItem);
        }

        BigDecimal deliveryAmount = rentalDeliveryFeeSupport.calc(
                previewBo.getPickupType(), previewBo.getPickupStoreId(), rentAmount);
        String deliveryFeeRemark = rentalDeliveryFeeSupport.buildRuleRemark(
                previewBo.getPickupType(), previewBo.getPickupStoreId(), rentAmount);
        BigDecimal couponAmount = userCouponService.previewDiscount(userId, previewBo.getCouponId(),
                rentAmount, deliveryAmount, depositAmount);
        BigDecimal payAmount = RentOrderPriceCalculator.calcPayAmount(
                rentAmount, depositAmount, deliveryAmount, couponAmount);

        RentCartCheckoutPreviewVo.ConfirmPayload payload = new RentCartCheckoutPreviewVo.ConfirmPayload();
        payload.setItems(confirmItems);
        payload.setPickupType(previewBo.getPickupType());
        payload.setPickupStoreId(previewBo.getPickupStoreId());
        payload.setAddressId(previewBo.getAddressId());
        payload.setCouponId(previewBo.getCouponId());
        payload.setCartIds(cartIds);
        payload.setRentAmount(rentAmount);
        payload.setDepositAmount(depositAmount);
        payload.setDeliveryAmount(deliveryAmount);
        payload.setCouponAmount(couponAmount);
        payload.setPayAmount(payAmount);

        RentCartCheckoutPreviewVo vo = new RentCartCheckoutPreviewVo();
        vo.setRentAmount(rentAmount);
        vo.setDepositAmount(depositAmount);
        vo.setDeliveryAmount(deliveryAmount);
        vo.setDeliveryFeeRemark(deliveryFeeRemark);
        vo.setCouponAmount(couponAmount);
        vo.setPayAmount(payAmount);
        vo.setPickupType(previewBo.getPickupType());
        vo.setPickupStoreId(previewBo.getPickupStoreId());
        vo.setAddressId(previewBo.getAddressId());
        vo.setItems(itemVos);
        vo.setConfirmPayload(payload);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByIds(Long userId, List<Long> cartIds) {
        if (CollectionUtils.isEmpty(cartIds)) {
            return;
        }
        rentCartMapper.delete(new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getUserId, userId)
                .in(RentCart::getId, cartIds));
    }

    private List<RentCart> loadCheckoutCarts(Long userId, List<Long> cartIds) {
        LambdaQueryWrapper<RentCart> wrapper = new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getUserId, userId);
        if (CollectionUtils.isNotEmpty(cartIds)) {
            wrapper.in(RentCart::getId, cartIds);
        } else {
            wrapper.eq(RentCart::getSelected, 1);
        }
        return rentCartMapper.selectList(wrapper);
    }

    private List<RentCartItemVo> buildItemVos(List<RentCart> carts) {
        List<RentCartItemVo> result = new ArrayList<>(carts.size());
        for (RentCart cart : carts) {
            result.add(buildSingleItemVo(cart));
        }
        return result;
    }

    private RentCartItemVo buildSingleItemVo(RentCart cart) {
        RentCartItemVo vo = new RentCartItemVo();
        vo.setId(cart.getId());
        vo.setGoodsId(cart.getGoodsId());
        vo.setSpecId(cart.getSpecId());
        vo.setPackageId(normalizePackageId(cart.getPackageId()));
        vo.setRentType(cart.getRentType());
        vo.setStartTime(cart.getStartTime());
        vo.setEndTime(cart.getEndTime());
        vo.setQty(cart.getQty());
        vo.setSelected(cart.getSelected() != null && cart.getSelected() == 1);

        fillSpecAndPackageInfo(vo, cart);

        RentGoods goods = rentGoodsMapper.selectById(cart.getGoodsId());
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            vo.setInvalid(true);
            vo.setInvalidReason("商品已下架");
            return vo;
        }
        if (!merchantStoreOpenSupport.isMerchantStoreOpen(goods.getMerchantId())) {
            vo.setInvalid(true);
            vo.setInvalidReason("店铺休息中");
            return vo;
        }
        vo.setGoodsName(goods.getName());
        vo.setGoodsNo(goods.getGoodsNo());
        vo.setCoverImage(goods.getCoverImage());
        vo.setRentPriceDay(goods.getRentPriceDay());
        vo.setRentPriceHour(goods.getRentPriceHour());
        vo.setRentPriceOvernight(goods.getRentPriceOvernight());
        vo.setDepositAmount(goods.getDepositAmount());

        RentGoodsSpec goodsSpec = resolveGoodsSpec(cart.getSpecId());
        RentGoodsPackage goodsPackage = resolveGoodsPackage(cart.getPackageId());

        try {
            int qty = cart.getQty() == null ? 1 : cart.getQty();
            vo.setItemRentAmount(RentOrderPriceCalculator.calcItemRentAmount(
                    goods, cart.getRentType(), cart.getStartTime(), cart.getEndTime(), null, qty, goodsPackage, goodsSpec));
            vo.setItemDepositAmount(RentOrderPriceCalculator.calcItemDeposit(goods, qty));

            RentStockCheckBo checkBo = new RentStockCheckBo();
            checkBo.setGoodsId(cart.getGoodsId());
            checkBo.setSpecId(cart.getSpecId());
            checkBo.setStartTime(cart.getStartTime());
            checkBo.setEndTime(cart.getEndTime());
            checkBo.setQuantity(qty);
            RentStockCheckVo checkVo = rentStockService.check(checkBo);
            if (!Boolean.TRUE.equals(checkVo.getAvailable())) {
                vo.setInvalid(true);
                vo.setInvalidReason("库存不足");
            } else {
                vo.setInvalid(false);
            }
        } catch (BizException ex) {
            vo.setInvalid(true);
            vo.setInvalidReason(ex.getMessage());
        }
        return vo;
    }

    private void fillSpecAndPackageInfo(RentCartItemVo vo, RentCart cart) {
        if (cart.getSpecId() != null && cart.getSpecId() > 0) {
            RentGoodsSpec spec = rentGoodsSpecMapper.selectById(cart.getSpecId());
            if (spec != null) {
                vo.setSpecInfo(spec.getSpecName());
            }
        }
        Long packageId = normalizePackageId(cart.getPackageId());
        if (packageId > 0) {
            RentGoodsPackage goodsPackage = rentGoodsPackageMapper.selectById(packageId);
            if (goodsPackage != null) {
                vo.setPackageName(goodsPackage.getPackageName());
            }
        }
    }

    private RentGoodsSpec resolveGoodsSpec(Long specId) {
        if (specId == null || specId <= 0) {
            return null;
        }
        return rentGoodsSpecMapper.selectById(specId);
    }

    private RentGoodsPackage resolveGoodsPackage(Long packageId) {
        Long normalized = normalizePackageId(packageId);
        if (normalized <= 0) {
            return null;
        }
        return rentGoodsPackageMapper.selectById(normalized);
    }

    private Long normalizePackageId(Long packageId) {
        return packageId == null ? 0L : packageId;
    }

    private RentCart requireOwnedCart(Long userId, Long cartId) {
        RentCart cart = rentCartMapper.selectOne(new LambdaQueryWrapper<RentCart>()
                .eq(RentCart::getId, cartId)
                .eq(RentCart::getUserId, userId)
                .last("LIMIT 1"));
        if (cart == null) {
            throw new BizException("购物车条目不存在");
        }
        return cart;
    }

    private RentGoods requireOnShelfGoods(Long goodsId) {
        RentGoods goods = rentGoodsMapper.selectById(goodsId);
        if (goods == null || goods.getStatus() == null || goods.getStatus() != 1) {
            throw new BizException("商品不存在或未上架");
        }
        merchantStoreOpenSupport.assertMerchantStoreOpen(goods.getMerchantId());
        return goods;
    }

    private void validateRentPeriod(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BizException("租期结束时间必须晚于开始时间");
        }
    }
}
