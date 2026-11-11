package com.sqx.app.rental.category.service;

import com.sqx.app.rental.category.domain.bo.RentCategorySaveBo;
import com.sqx.app.rental.category.domain.vo.RentCategoryInfoVo;
import com.sqx.app.rental.category.domain.vo.RentCategoryTreeVo;

import java.util.List;

/**
 * 租赁分类服务
 */
public interface RentCategoryService {

    /**
     * 查询启用状态的分类树（用户端）
     */
    List<RentCategoryTreeVo> tree();

    /**
     * 查询全部分类树（管理端，含禁用项）
     */
    List<RentCategoryTreeVo> adminTree();

    RentCategoryInfoVo info(Long id);

    void save(RentCategorySaveBo saveBo);

    void update(RentCategorySaveBo saveBo);

    void delete(Long id);
}
