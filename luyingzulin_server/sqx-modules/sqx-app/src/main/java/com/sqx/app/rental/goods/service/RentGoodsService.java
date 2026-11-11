package com.sqx.app.rental.goods.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.goods.domain.bo.RentGoodsInfoBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsPageBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsSaveBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateBo;
import com.sqx.app.rental.goods.domain.bo.RentGoodsUpdateStateBo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsDetailVo;
import com.sqx.app.rental.goods.domain.vo.RentGoodsListVo;

/**
 * 租赁商品服务
 */
public interface RentGoodsService {

    /**
     * 分页查询商品
     */
    IPage<RentGoodsListVo> page(RentGoodsPageBo pageBo);

    /**
     * 商品详情
     */
    RentGoodsDetailVo detail(Long id);

    /**
     * 新增商品
     */
    void saveInfo(RentGoodsSaveBo saveBo);

    /**
     * 更新商品
     */
    void updateInfo(RentGoodsUpdateBo updateBo);

    /**
     * 删除商品
     */
    void delete(RentGoodsInfoBo infoBo);

    /**
     * 上下架
     */
    void updateState(RentGoodsUpdateStateBo updateStateBo);
}
