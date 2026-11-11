package com.sqx.app.rental.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sqx.app.rental.search.domain.entity.SearchHotKeyword;
import com.sqx.app.rental.search.domain.vo.SearchHotKeywordVo;
import com.sqx.app.rental.search.mapper.SearchHotKeywordMapper;
import com.sqx.app.rental.search.service.RentSearchService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 热门搜索服务实现
 */
@Service
public class RentSearchServiceImpl implements RentSearchService {

    @Resource
    private SearchHotKeywordMapper searchHotKeywordMapper;

    @Override
    public List<SearchHotKeywordVo> hotKeywords() {
        List<SearchHotKeyword> keywords = searchHotKeywordMapper.selectList(new LambdaQueryWrapper<SearchHotKeyword>()
                .eq(SearchHotKeyword::getStatus, 1)
                .orderByAsc(SearchHotKeyword::getSort)
                .orderByAsc(SearchHotKeyword::getId));
        if (CollectionUtils.isEmpty(keywords)) {
            return Collections.emptyList();
        }
        return keywords.stream().map(item -> {
            SearchHotKeywordVo vo = new SearchHotKeywordVo();
            vo.setKeyword(item.getKeyword());
            return vo;
        }).collect(Collectors.toList());
    }
}
