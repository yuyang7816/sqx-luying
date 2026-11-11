package com.sqx.app.rental.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.domain.bo.UpdateUserBO;
import com.sqx.app.rental.user.domain.bo.AdminUserPageBo;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.rental.user.domain.vo.AppUserInfoVo;

/**
 * 管理端 C 端用户服务
 */
public interface AdminRentalUserService {

    Page<AdminUserListVo> page(AdminUserPageBo pageBo);

    AppUserInfoVo info(Long userId);

    void update(UpdateUserBO updateUserBO);

    void delete(Long userId);
}
