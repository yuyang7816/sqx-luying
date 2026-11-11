package com.sqx.common.shiro.service.impl;


import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.sqx.common.core.constant.Constant;
import com.sqx.common.core.constant.Constant;
import com.sqx.common.redis.utils.RedisUtil;
import com.sqx.common.shiro.constant.ShiroConstant;
import com.sqx.common.shiro.model.AuthSysMenu;
import com.sqx.common.shiro.service.ShiroService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author chengming
 * @since 2024/7/11
 */
@Service
public class ShiroServiceImpl implements ShiroService {
    @Override
    public Set<String> getUserPermissions(long userId) {
        if (userId == Constant.SUPER_ADMIN) {
            Set<String> superPerms = new HashSet<>();
            superPerms.add("*");
            return superPerms;
        }
        List<String> permsList = new ArrayList<>();
        String cacheKey = String.format(ShiroConstant.AUTH_USER_MENU,userId);
        String menuListJson = RedisUtil.getCacheObject(cacheKey);
        if (StringUtils.isNotBlank(menuListJson)){
            List<AuthSysMenu> menuList = JSONObject.parseObject(menuListJson, new TypeReference<List<AuthSysMenu>>() {});
            if (menuList != null){
                for (AuthSysMenu menu : menuList) {
                    if (StringUtils.isNotBlank(menu.getPerms())){
                        permsList.add(menu.getPerms());
                    }
                }
            }
        }
        //用户权限列表
        Set<String> permsSet = new HashSet<>();
        for (String perms : permsList) {
            if (StringUtils.isBlank(perms)) {
                continue;
            }
            permsSet.addAll(Arrays.asList(perms.trim().split(",")));
        }
        return permsSet;
    }
}
