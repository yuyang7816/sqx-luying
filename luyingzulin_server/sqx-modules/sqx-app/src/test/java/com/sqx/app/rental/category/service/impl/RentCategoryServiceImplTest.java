package com.sqx.app.rental.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.rental.category.domain.entity.RentCategory;
import com.sqx.app.rental.category.domain.vo.RentCategoryTreeVo;
import com.sqx.app.rental.category.mapper.RentCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M01：分类树构建单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentCategoryServiceImplTest {

    @Mock
    private RentCategoryMapper rentCategoryMapper;

    @InjectMocks
    private RentCategoryServiceImpl rentCategoryService;

    @Test
    void treeShouldReturnEmptyWhenNoCategories() {
        when(rentCategoryMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<RentCategoryTreeVo> result = rentCategoryService.tree();

        assertTrue(result.isEmpty());
        verify(rentCategoryMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void treeShouldBuildTwoLevelHierarchy() {
        RentCategory root = category(1L, 0L, "露营装备", 1);
        RentCategory childA = category(11L, 1L, "帐篷", 1);
        RentCategory childB = category(12L, 1L, "睡袋", 2);
        RentCategory root2 = category(2L, 0L, "户外服饰", 2);

        when(rentCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(root, childA, childB, root2));

        List<RentCategoryTreeVo> result = rentCategoryService.tree();

        assertEquals(2, result.size());
        assertEquals("露营装备", result.get(0).getName());
        assertEquals(2, result.get(0).getChildren().size());
        assertEquals("帐篷", result.get(0).getChildren().get(0).getName());
        assertEquals("睡袋", result.get(0).getChildren().get(1).getName());
        assertTrue(result.get(0).getChildren().get(0).getChildren().isEmpty());
        assertEquals("户外服饰", result.get(1).getName());
        assertTrue(result.get(1).getChildren().isEmpty());
    }

    @Test
    void treeShouldTreatNullParentAsRoot() {
        RentCategory root = category(1L, null, "根分类", 1);

        when(rentCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(root));

        List<RentCategoryTreeVo> result = rentCategoryService.tree();

        assertEquals(1, result.size());
        assertEquals("根分类", result.get(0).getName());
        assertTrue(result.get(0).getChildren().isEmpty());
    }

    private static RentCategory category(Long id, Long parentId, String name, int sort) {
        RentCategory category = new RentCategory();
        category.setId(id);
        category.setParentId(parentId);
        category.setName(name);
        category.setSort(sort);
        category.setStatus(1);
        return category;
    }
}
