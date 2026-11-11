package com.sqx.app.rental.search.service;

import com.sqx.app.rental.search.domain.vo.SearchHotKeywordVo;

import java.util.List;

/**
 * 热门搜索服务
 */
public interface RentSearchService {

    /**
     * 热门搜索词列表
     */
    List<SearchHotKeywordVo> hotKeywords();
}
