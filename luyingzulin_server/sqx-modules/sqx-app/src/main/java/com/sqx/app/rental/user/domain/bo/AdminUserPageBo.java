package com.sqx.app.rental.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理端 C 端用户分页筛选
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "管理端C端用户分页筛选")
public class AdminUserPageBo extends PageParam {

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "状态 1正常 2禁用")
    private Integer status;

    @Schema(description = "用户类型 1普通用户 2vip用户")
    private Integer userType;

    @Schema(description = "是否是会员 0否 1是")
    private Integer isVip;

    @Schema(description = "注册来源")
    private String platform;

    @Schema(description = "邀请码")
    private String invitationCode;

    @Schema(description = "注册开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeStart;

    @Schema(description = "注册结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTimeEnd;
}
