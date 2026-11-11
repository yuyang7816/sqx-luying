package com.sqx.app.rental.user.service;

import com.sqx.app.rental.user.domain.vo.UserFavoriteToggleVo;
import com.sqx.app.rental.user.domain.vo.UserFavoriteVo;

import java.util.List;

/**
 * 用户商品收藏服务
 */
public interface UserFavoriteService {

    List<UserFavoriteVo> list(Long userId);

    UserFavoriteToggleVo toggle(Long userId, Long goodsId);

    boolean check(Long userId, Long goodsId);
}
