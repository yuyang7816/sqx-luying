package com.sqx.app.referral.commission.domain.bo;

import com.sqx.common.mybatis.domain.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-04 14:44:40
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ReferralCommissionPageBo extends PageParam {

    private Long userId;

    private String phone;
}
