package com.sqx.app.rental.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sqx.app.rental.user.domain.entity.UserFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /** 清理同用户同商品的历史软删行，避免 uk(user_id, goods_id, deleted) 冲突 */
    @Delete("DELETE FROM user_favorite WHERE user_id = #{userId} AND goods_id = #{goodsId} AND deleted = -1")
    int purgeSoftDeleted(@Param("userId") Long userId, @Param("goodsId") Long goodsId);

    /** 恢复软删收藏，避免重复 insert 触发唯一键冲突 */
    @Update("UPDATE user_favorite SET deleted = 0, update_time = NOW() "
            + "WHERE user_id = #{userId} AND goods_id = #{goodsId} AND deleted = -1 LIMIT 1")
    int restoreSoftDeleted(@Param("userId") Long userId, @Param("goodsId") Long goodsId);
}
