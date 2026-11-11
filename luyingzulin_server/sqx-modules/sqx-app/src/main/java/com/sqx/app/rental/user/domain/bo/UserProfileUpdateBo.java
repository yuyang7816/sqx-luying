package com.sqx.app.rental.user.domain.bo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户资料更新
 */
@Data
@Schema(description = "用户资料更新")
public class UserProfileUpdateBo {

    @Schema(description = "用户名/昵称")
    private String userName;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别 1男 2女")
    private Integer sex;

    @Schema(description = "年龄")
    private Integer age;
}
