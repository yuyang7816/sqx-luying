package com.sqx.app.vip.rights.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.vip.rights.db.service.IVipRightsService;
import com.sqx.app.vip.rights.domain.bo.VipRightsInfoBo;
import com.sqx.app.vip.rights.domain.bo.VipRightsPageBo;
import com.sqx.app.vip.rights.domain.bo.VipRightsSaveBo;
import com.sqx.app.vip.rights.domain.bo.VipRightsUpdateBo;
import com.sqx.app.vip.rights.domain.entity.VipRights;
import com.sqx.app.vip.rights.domain.vo.VipRightsVo;
import com.sqx.app.vip.rights.service.VipRightsService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会员特权 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-07 16:56:11
 */
@Service
public class VipRightsServiceImpl implements VipRightsService {

    @Resource
    private IVipRightsService iVipRightsService;

    @Override
    public void saveInfo(VipRightsSaveBo vipRightsSaveBo) {
        VipRights vipRights = new VipRights();
        BeanUtils.copyProperties(vipRightsSaveBo, vipRights);

        iVipRightsService.save(vipRights);
    }

    @Override
    public IPage<VipRightsVo> pageInfo(VipRightsPageBo vipRightsPageBo) {
        Page<VipRights> initPage = PageUtil.initPage(vipRightsPageBo.getPage(), vipRightsPageBo.getLimit());

        QueryWrapper<VipRights> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .orderByDesc(VipRights::getCreateTime);

        IPage<VipRights> iPage = iVipRightsService.page(initPage, queryWrapper);
        List<VipRightsVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (VipRights record : iPage.getRecords()) {
                VipRightsVo vo = new VipRightsVo();
                BeanUtils.copyProperties(record, vo);

                voList.add(vo);
            }
        }

        IPage<VipRightsVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public VipRightsVo info(VipRightsInfoBo vipRightsInfoBo) {
        VipRights vipRights = iVipRightsService.getById(vipRightsInfoBo.getId());
        if (Objects.isNull(vipRights)) {
            throw new BizException("信息不存在");
        }

        VipRightsVo vo = new VipRightsVo();
        BeanUtils.copyProperties(vipRights, vo);
        return vo;
    }

    @Override
    public void updateInfo(VipRightsUpdateBo vipRightsUpdateBo) {
        VipRights vipRights = new VipRights();
        BeanUtils.copyProperties(vipRightsUpdateBo, vipRights);

        iVipRightsService.updateById(vipRights);
    }

    @Override
    public void delete(VipRightsInfoBo vipRightsInfoBo) {
        iVipRightsService.removeById(vipRightsInfoBo.getId());
    }

    @Override
    public List<VipRightsVo> list() {
        List<VipRights> vipRightsList = iVipRightsService.list();

        List<VipRightsVo> voList = CollectionUtils.isNotEmpty(vipRightsList) ? vipRightsList.stream().map(vipRights -> {
            VipRightsVo vo = new VipRightsVo();
            BeanUtils.copyProperties(vipRights, vo);
            return vo;
        }).collect(Collectors.toList()) : Collections.emptyList();

        return voList;
    }
}
