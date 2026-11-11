package com.sqx.app.rental.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.rental.category.domain.bo.RentCategorySaveBo;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.domain.vo.RentCategoryInfoVo;
import com.sqx.app.rental.category.domain.vo.RentCategoryTreeVo;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import com.sqx.app.rental.category.service.RentCategoryService;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.common.core.exception.BizException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 租赁分类服务实现
 */
@Service
public class RentCategoryServiceImpl implements RentCategoryService {

    @Resource
    private RentCategoryMapper rentCategoryMapper;

    @Resource
    private RentGoodsMapper rentGoodsMapper;

    @Override
    public List<RentCategoryTreeVo> tree() {
        List<RentCategory> categories = rentCategoryMapper.selectList(new LambdaQueryWrapper<RentCategory>()
                .eq(RentCategory::getStatus, 1)
                .orderByAsc(RentCategory::getSort)
                .orderByAsc(RentCategory::getId));
        return buildTree(categories);
    }

    @Override
    public List<RentCategoryTreeVo> adminTree() {
        List<RentCategory> categories = rentCategoryMapper.selectList(new LambdaQueryWrapper<RentCategory>()
                .orderByAsc(RentCategory::getSort)
                .orderByAsc(RentCategory::getId));
        return buildTree(categories);
    }

    @Override
    public RentCategoryInfoVo info(Long id) {
        RentCategory category = requireCategory(id);
        RentCategoryInfoVo vo = new RentCategoryInfoVo();
        BeanUtils.copyProperties(category, vo);
        if (vo.getParentId() == null) {
            vo.setParentId(0L);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(RentCategorySaveBo saveBo) {
        Long parentId = normalizeParentId(saveBo.getParentId());
        validateParentId(parentId, null);
        RentCategory category = new RentCategory();
        category.setParentId(parentId);
        category.setName(StringUtils.trim(saveBo.getName()));
        category.setIcon(StringUtils.trimToEmpty(saveBo.getIcon()));
        category.setSort(saveBo.getSort() == null ? 0 : saveBo.getSort());
        category.setStatus(saveBo.getStatus() == null ? 1 : saveBo.getStatus());
        rentCategoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RentCategorySaveBo saveBo) {
        if (saveBo.getId() == null) {
            throw new BizException("分类ID不能为空");
        }
        RentCategory category = requireCategory(saveBo.getId());
        Long parentId = normalizeParentId(saveBo.getParentId());
        validateUpdateParent(category, parentId);
        category.setParentId(parentId);
        category.setName(StringUtils.trim(saveBo.getName()));
        category.setIcon(StringUtils.trimToEmpty(saveBo.getIcon()));
        if (saveBo.getSort() != null) {
            category.setSort(saveBo.getSort());
        }
        if (saveBo.getStatus() != null) {
            category.setStatus(saveBo.getStatus());
        }
        rentCategoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCategory(id);
        Long childCount = rentCategoryMapper.selectCount(new LambdaQueryWrapper<RentCategory>()
                .eq(RentCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("请先删除子分类");
        }
        Long goodsCount = rentGoodsMapper.selectCount(new LambdaQueryWrapper<RentGoods>()
                .eq(RentGoods::getCategoryId, id));
        if (goodsCount != null && goodsCount > 0) {
            throw new BizException("该分类下仍有关联商品，无法删除");
        }
        rentCategoryMapper.deleteById(id);
    }

    private List<RentCategoryTreeVo> buildTree(List<RentCategory> categories) {
        if (CollectionUtils.isEmpty(categories)) {
            return Collections.emptyList();
        }

        Map<Long, List<RentCategory>> childrenMap = categories.stream()
                .filter(item -> item.getParentId() != null && item.getParentId() > 0)
                .collect(Collectors.groupingBy(RentCategory::getParentId));

        List<RentCategoryTreeVo> roots = new ArrayList<>();
        for (RentCategory category : categories) {
            if (category.getParentId() == null || category.getParentId() == 0) {
                RentCategoryTreeVo root = toTreeVo(category);
                root.setChildren(buildChildren(childrenMap.get(category.getId())));
                roots.add(root);
            }
        }
        return roots;
    }

    private List<RentCategoryTreeVo> buildChildren(List<RentCategory> children) {
        if (CollectionUtils.isEmpty(children)) {
            return Collections.emptyList();
        }
        List<RentCategoryTreeVo> voList = new ArrayList<>();
        for (RentCategory child : children) {
            RentCategoryTreeVo vo = toTreeVo(child);
            vo.setChildren(Collections.emptyList());
            voList.add(vo);
        }
        return voList;
    }

    private RentCategoryTreeVo toTreeVo(RentCategory category) {
        RentCategoryTreeVo vo = new RentCategoryTreeVo();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }

    private RentCategory requireCategory(Long id) {
        RentCategory category = rentCategoryMapper.selectById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        return category;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private void validateParentId(Long parentId, Long excludeId) {
        if (parentId == 0) {
            return;
        }
        if (excludeId != null && parentId.equals(excludeId)) {
            throw new BizException("不能将自己设为上级分类");
        }
        RentCategory parent = rentCategoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException("上级分类不存在");
        }
        if (parent.getParentId() != null && parent.getParentId() > 0) {
            throw new BizException("仅支持两级分类，子分类不能再挂子级");
        }
    }

    private void validateUpdateParent(RentCategory category, Long newParentId) {
        Long oldParentId = normalizeParentId(category.getParentId());
        if (newParentId.equals(oldParentId)) {
            return;
        }
        validateParentId(newParentId, category.getId());
        Long childCount = rentCategoryMapper.selectCount(new LambdaQueryWrapper<RentCategory>()
                .eq(RentCategory::getParentId, category.getId()));
        if (childCount != null && childCount > 0 && newParentId > 0) {
            throw new BizException("该分类下仍有子分类，不能调整为子级");
        }
    }
}
