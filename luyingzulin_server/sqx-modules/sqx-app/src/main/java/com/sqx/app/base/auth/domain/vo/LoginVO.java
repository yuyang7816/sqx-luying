package com.sqx.app.base.auth.domain.vo;

import com.sqx.app.base.user.domain.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author chengming
 * @since 2025/4/22
 */
@Data
public class LoginVO {

    @Schema(description = "token")
    private String token;

    @Schema(description = "token过期时间")
    private Long expire;

    @Schema(description = "用户信息")
    private UserEntity user;
}
