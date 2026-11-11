package com.sqx.app.referral.user.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "管理端用户佣金分页")
public class ReferralUserCommissionPageBo extends PageParam {

    private String phone;

    private String userName;
}
