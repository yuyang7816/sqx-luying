package com.sqx.system.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.common.mybatis.utils.PageUtils;
import com.sqx.common.mybatis.utils.Query;
import com.sqx.system.db.entity.SysDictEntity;
import com.sqx.system.db.service.ISysDictService;
import com.sqx.system.service.SysDictService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("sysDictService")
public class SysDictServiceImpl implements SysDictService {
    @Autowired
    private ISysDictService sysDictService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String name = (String)params.get("name");

        IPage<SysDictEntity> page = sysDictService.page(
            new Query<SysDictEntity>().getPage(params),
            new QueryWrapper<SysDictEntity>()
                .like(StringUtils.isNotBlank(name),"name", name)
        );

        return new PageUtils(page);
    }

}
