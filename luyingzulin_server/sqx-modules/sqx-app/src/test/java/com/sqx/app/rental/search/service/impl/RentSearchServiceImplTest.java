package com.sqx.app.rental.search.service.impl;

import com.sqx.app.rental.search.domain.entity.SearchHotKeyword;
import com.sqx.app.rental.search.domain.vo.SearchHotKeywordVo;
import com.sqx.app.rental.search.mapper.SearchHotKeywordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M02：热门搜索单元测试
 */
@ExtendWith(MockitoExtension.class)
class RentSearchServiceImplTest {

    @Mock
    private SearchHotKeywordMapper searchHotKeywordMapper;

    @InjectMocks
    private RentSearchServiceImpl rentSearchService;

    @Test
    void hotKeywordsShouldReturnEnabledKeywordsInSortOrder() {
        SearchHotKeyword first = new SearchHotKeyword();
        first.setKeyword("帐篷");
        SearchHotKeyword second = new SearchHotKeyword();
        second.setKeyword("天幕");
        when(searchHotKeywordMapper.selectList(any())).thenReturn(Arrays.asList(first, second));

        List<SearchHotKeywordVo> result = rentSearchService.hotKeywords();

        assertEquals(2, result.size());
        assertEquals("帐篷", result.get(0).getKeyword());
        assertEquals("天幕", result.get(1).getKeyword());
    }
}
