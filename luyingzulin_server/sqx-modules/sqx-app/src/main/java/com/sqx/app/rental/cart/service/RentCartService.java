package com.sqx.app.rental.cart.service;

import com.sqx.app.rental.cart.domain.bo.*;
import com.sqx.app.rental.cart.domain.vo.RentCartCheckoutPreviewVo;
import com.sqx.app.rental.cart.domain.vo.RentCartItemVo;

import java.util.List;

/**
 * 租赁购物车服务
 */
public interface RentCartService {

    List<RentCartItemVo> list(Long userId);

    Long add(Long userId, RentCartAddBo addBo);

    void update(Long userId, RentCartUpdateBo updateBo);

    void delete(Long userId, Long id);

    void deleteBatch(Long userId, RentCartDeleteBo deleteBo);

    void select(Long userId, RentCartSelectBo selectBo);

    RentCartCheckoutPreviewVo checkoutPreview(Long userId, RentCartCheckoutPreviewBo previewBo);

    void removeByIds(Long userId, List<Long> cartIds);
}
