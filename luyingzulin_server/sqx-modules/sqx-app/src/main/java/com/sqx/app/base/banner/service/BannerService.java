package com.sqx.app.base.banner.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.banner.domain.bo.*;
import com.sqx.app.base.banner.domain.vo.BannerVo;
import com.sqx.common.core.domain.Result;

/**
 * @author chengming
 * @since 2025/5/16
 */
public interface BannerService {

    void saveInfo(BannerSaveBo bannerSaveBo);

    IPage<BannerVo> pageInfo(BannerPageBo bannerPageBo);

    /**
     * 用户端 Banner 分页（按 sort 升序，供首页轮播）
     */
    IPage<BannerVo> pageForApp(BannerPageBo bannerPageBo);

    BannerVo info(BannerInfoBo bannerInfoBo);

    void updateInfo(BannerUpdateBo bannerUpdateBo);

    void delete(BannerInfoBo bannerInfoBo);

    void updateState(BannerUpdateStateBo bannerUpdateStateBo);
}
