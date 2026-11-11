package com.sqx.app.vip.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.vip.plan.db.service.IVipPlanService;
import com.sqx.app.vip.plan.domain.bo.VipPlanInfoBo;
import com.sqx.app.vip.plan.domain.bo.VipPlanPageBo;
import com.sqx.app.vip.plan.domain.bo.VipPlanSaveBo;
import com.sqx.app.vip.plan.domain.bo.VipPlanUpdateBo;
import com.sqx.app.vip.plan.domain.entity.VipPlan;
import com.sqx.app.vip.plan.domain.vo.VipPlanVo;
import com.sqx.app.vip.plan.service.VipPlanService;
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
 * 会员套餐	 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-07 16:51:13
 */
@Service
public class VipPlanServiceImpl implements VipPlanService {

    @Resource
    private IVipPlanService iVipPlanService;

    @Override
    public void saveInfo(VipPlanSaveBo vipPlanSaveBo) {
        VipPlan vipPlan = new VipPlan();
        BeanUtils.copyProperties(vipPlanSaveBo, vipPlan);

        iVipPlanService.save(vipPlan);
    }

    @Override
    public IPage<VipPlanVo> pageInfo(VipPlanPageBo vipPlanPageBo) {
        Page<VipPlan> initPage = PageUtil.initPage(vipPlanPageBo.getPage(), vipPlanPageBo.getLimit());

        QueryWrapper<VipPlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .orderByDesc(VipPlan::getCreateTime);

        IPage<VipPlan> iPage = iVipPlanService.page(initPage, queryWrapper);
        List<VipPlanVo> voList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(iPage.getRecords())) {
            for (VipPlan record : iPage.getRecords()) {
                VipPlanVo vo = new VipPlanVo();
                BeanUtils.copyProperties(record, vo);

                voList.add(vo);
            }
        }

        IPage<VipPlanVo> voIPage = PageUtil.copyPage(iPage, voList);
        return voIPage;
    }

    @Override
    public VipPlanVo info(VipPlanInfoBo vipPlanInfoBo) {
        VipPlan vipPlan = iVipPlanService.getById(vipPlanInfoBo.getId());
        if (Objects.isNull(vipPlan)) {
            throw new BizException("信息不存在");
        }

        VipPlanVo vo = new VipPlanVo();
        BeanUtils.copyProperties(vipPlan, vo);
        return vo;
    }

    @Override
    public void updateInfo(VipPlanUpdateBo vipPlanUpdateBo) {
        VipPlan vipPlan = new VipPlan();
        BeanUtils.copyProperties(vipPlanUpdateBo, vipPlan);

        iVipPlanService.updateById(vipPlan);
    }

    @Override
    public void delete(VipPlanInfoBo vipPlanInfoBo) {
        iVipPlanService.removeById(vipPlanInfoBo.getId());
    }

    @Override
    public List<VipPlanVo> list() {
        List<VipPlan> vipPlanList = iVipPlanService.list();

        List<VipPlanVo> voList = CollectionUtils.isNotEmpty(vipPlanList) ? vipPlanList.stream().map(vipPlan -> {
            VipPlanVo vo = new VipPlanVo();
            BeanUtils.copyProperties(vipPlan, vo);
            return vo;
        }).collect(Collectors.toList()) : Collections.emptyList();

        return voList;
    }
}
