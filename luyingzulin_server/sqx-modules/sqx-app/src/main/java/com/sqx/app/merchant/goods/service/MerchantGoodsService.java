package com.sqx.app.merchant.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsPageBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantGoodsShelfBo;
import com.sqx.app.merchant.goods.domain.bo.MerchantStockAdjustBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSaveBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;

public interface MerchantGoodsService {

    IPage<RentGoodsListVo> page(Long userId, MerchantGoodsPageBo pageBo);

    RentGoodsDetailVo detail(Long userId, Long goodsId);

    void save(Long userId, RentGoodsSaveBo saveBo);

    void update(Long userId, RentGoodsUpdateBo updateBo);

    void shelf(Long userId, MerchantGoodsShelfBo shelfBo);

    void adjustStock(Long userId, MerchantStockAdjustBo adjustBo);

    void delete(Long userId, Long goodsId);
}
