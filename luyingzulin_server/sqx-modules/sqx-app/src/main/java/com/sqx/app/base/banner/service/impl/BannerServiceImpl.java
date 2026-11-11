package com.sqx.app.base.banner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.banner.domain.bo.*;
import com.sqx.app.base.banner.domain.vo.BannerVo;
import com.sqx.app.base.banner.service.BannerService;
import com.sqx.app.base.enums.AppError;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import com.sqx.system.db.entity.Banner;
import com.sqx.system.db.service.IBannerService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author chengming
 * @since 2025/5/16
 */
@Service
public class BannerServiceImpl implements BannerService {
    @Resource
    private IBannerService iBannerService;

    @Override
    public void saveInfo(BannerSaveBo bannerSaveBo) {
        Banner banner = new Banner();
        BeanUtils.copyProperties(bannerSaveBo, banner);
        banner.setState(1);

        iBannerService.save(banner);
    }

    @Override
    public IPage<BannerVo> pageInfo(BannerPageBo bannerPageBo) {
        Page<Banner> initPage = PageUtil.initPage(bannerPageBo.getPage(), bannerPageBo.getLimit());

        QueryWrapper<Banner> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(bannerPageBo.getName()), Banner::getName, bannerPageBo.getName())
                .eq(Objects.nonNull(bannerPageBo.getClassify()), Banner::getClassify, bannerPageBo.getClassify())
                .eq(Objects.nonNull(bannerPageBo.getState()), Banner::getState, bannerPageBo.getState())
                .orderByDesc(Banner::getCreateTime);

        IPage<Banner> iPage = iBannerService.page(initPage, queryWrapper);
        List<BannerVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (Banner record : iPage.getRecords()) {
                BannerVo vo = new BannerVo();
                BeanUtils.copyProperties(record, vo);

                voList.add(vo);
            }
        }

        IPage<BannerVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public IPage<BannerVo> pageForApp(BannerPageBo bannerPageBo) {
        Page<Banner> initPage = PageUtil.initPage(bannerPageBo.getPage(), bannerPageBo.getLimit());

        QueryWrapper<Banner> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .like(StringUtils.isNotBlank(bannerPageBo.getName()), Banner::getName, bannerPageBo.getName())
                .eq(Objects.nonNull(bannerPageBo.getClassify()), Banner::getClassify, bannerPageBo.getClassify())
                .eq(Objects.nonNull(bannerPageBo.getState()), Banner::getState, bannerPageBo.getState())
                .orderByAsc(Banner::getSort)
                .orderByDesc(Banner::getId);

        IPage<Banner> iPage = iBannerService.page(initPage, queryWrapper);
        List<BannerVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (Banner record : iPage.getRecords()) {
                BannerVo vo = new BannerVo();
                BeanUtils.copyProperties(record, vo);
                voList.add(vo);
            }
        }
        return PageUtil.copyPage(iPage, voList);
    }

    @Override
    public BannerVo info(BannerInfoBo bannerInfoBo) {
        Banner banner = iBannerService.getById(bannerInfoBo.getId());
        if (Objects.isNull(banner)) {
            throw new BizException(AppError.INFO_NOT_FOUND);
        }

        BannerVo vo = new BannerVo();
        BeanUtils.copyProperties(banner, vo);
        return vo;
    }

    @Override
    public void updateInfo(BannerUpdateBo bannerUpdateBo) {
        Banner banner = new Banner();
        BeanUtils.copyProperties(bannerUpdateBo, banner);

        iBannerService.updateById(banner);
    }

    @Override
    public void delete(BannerInfoBo bannerInfoBo) {
        iBannerService.removeById(bannerInfoBo.getId());
    }

    @Override
    public void updateState(BannerUpdateStateBo bannerUpdateStateBo) {
        LambdaUpdateWrapper<Banner> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .eq(Banner::getId, bannerUpdateStateBo.getId())
                .set(Banner::getState, bannerUpdateStateBo.getState());

        iBannerService.update(new Banner(), updateWrapper);
    }
}
