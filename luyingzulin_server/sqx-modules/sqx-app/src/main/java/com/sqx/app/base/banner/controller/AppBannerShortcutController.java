package com.sqx.app.base.banner.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.banner.domain.bo.BannerPageBo;
import com.sqx.app.base.banner.domain.vo.BannerVo;
import com.sqx.app.base.banner.service.BannerService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户端 Banner 快捷入口（TabBar 首页轮播）
 * <p>
 * 与 {@link AppBannerController} 复用同一服务，路径对齐前端约定 {@code GET /app/banner/page}。
 */
@Tag(name = "用户端-banner", description = "TabBar 首页轮播")
@RestController
@RequestMapping("/app/banner")
public class AppBannerShortcutController {

    @Resource
    private BannerService bannerService;

    @Operation(summary = "首页轮播分页", description = "默认返回启用状态的首页 Banner（classify=1），按 sort 升序")
    @ApiResponse(content = @Content(schema = @Schema(implementation = BannerVo.class)))
    @GetMapping("/page")
    public Result<IPage<BannerVo>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) Integer classify) {
        BannerPageBo pageBo = new BannerPageBo();
        pageBo.setPage(page.longValue());
        pageBo.setLimit(limit.longValue());
        pageBo.setClassify(classify != null ? classify : 1);
        pageBo.setState(1);
        return Result.success(bannerService.pageForApp(pageBo));
    }
}
