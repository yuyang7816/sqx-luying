package com.sqx.app.rental.search.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 热门搜索词
 */
@Data
@Schema(description = "热门搜索词")
public class SearchHotKeywordVo {

    @Schema(description = "搜索关键词")
    private String keyword;
}
