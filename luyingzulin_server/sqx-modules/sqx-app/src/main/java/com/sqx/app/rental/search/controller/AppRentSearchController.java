package com.sqx.app.rental.search.controller;

import com.sqx.app.rental.search.domain.vo.SearchHotKeywordVo;
import com.sqx.app.rental.search.service.RentSearchService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 用户端-租赁搜索
 */
@Tag(name = "用户端-租赁搜索", description = "热门搜索词")
@RestController
@RequestMapping("/app/rental/search")
public class AppRentSearchController {

    @Resource
    private RentSearchService rentSearchService;

    @Operation(summary = "热门搜索词", description = "搜索页热门关键词")
    @ApiResponse(content = @Content(schema = @Schema(implementation = SearchHotKeywordVo.class)))
    @GetMapping("/hot")
    public Result<List<SearchHotKeywordVo>> hot() {
        return Result.success(rentSearchService.hotKeywords());
    }
}
