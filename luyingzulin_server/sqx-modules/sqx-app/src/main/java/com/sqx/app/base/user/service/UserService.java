package com.sqx.app.base.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.domain.bo.DeleteUserBO;
import com.sqx.app.base.user.domain.bo.ForgetPasswordBO;
import com.sqx.app.base.user.domain.bo.ListUserBO;
import com.sqx.app.base.user.domain.bo.UpdateUserBO;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.domain.vo.UserVo;
import com.sqx.common.core.domain.Result;

import java.util.List;

/**
 * @author chengming
 * @since 2025/4/24
 */
public interface UserService {
    Result<UserVo> info(Long userId);

    Result<Void> forgetPassword(ForgetPasswordBO forgetPasswordBO);

    Page<UserEntity> list(ListUserBO listUserBO);

    void update(UpdateUserBO updateUserBO);

    void delete(DeleteUserBO deleteUserBO);
}
