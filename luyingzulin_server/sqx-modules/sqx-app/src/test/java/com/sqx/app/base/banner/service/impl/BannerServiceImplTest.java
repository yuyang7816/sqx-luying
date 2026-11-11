package com.sqx.app.base.banner.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.banner.domain.bo.BannerPageBo;
import com.sqx.app.base.banner.domain.vo.BannerVo;
import com.sqx.system.db.entity.Banner;
import com.sqx.system.db.service.IBannerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * M01：Banner 用户端分页单元测试
 */
@ExtendWith(MockitoExtension.class)
class BannerServiceImplTest {

    @Mock
    private IBannerService iBannerService;

    @InjectMocks
    private BannerServiceImpl bannerService;

    @Test
    void pageForAppShouldReturnEnabledBanners() {
        Banner banner = new Banner();
        banner.setId(1L);
        banner.setName("首页活动");
        banner.setState(1);
        banner.setSort(1);

        Page<Banner> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(banner));
        page.setTotal(1);

        when(iBannerService.page(any(), any())).thenReturn(page);

        BannerPageBo pageBo = new BannerPageBo();
        pageBo.setPage(1L);
        pageBo.setLimit(10L);
        pageBo.setClassify(1);
        pageBo.setState(1);

        IPage<BannerVo> result = bannerService.pageForApp(pageBo);

        assertEquals(1, result.getRecords().size());
        assertEquals("首页活动", result.getRecords().get(0).getName());
    }
}
