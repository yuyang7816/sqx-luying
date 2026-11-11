package com.sqx.app.referral.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.referral.commission.domain.vo.ReferralListVo;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionInfoBo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionPageBo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionUpdateBo;
import com.sqx.app.referral.user.domain.vo.AdminReferralUserCommissionListVo;
import com.sqx.app.referral.user.domain.vo.ReferralDataVo;
import com.sqx.app.referral.user.domain.vo.ReferralLawyerVo;
import com.sqx.app.referral.user.domain.vo.ReferralTeamMemberVo;
import com.sqx.app.referral.user.domain.vo.ReferralTeamOverviewVo;
import com.sqx.app.referral.user.domain.vo.ReferralUserCommissionVo;


/**
 * 用户佣金 服务类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-04 14:44:53
 */
public interface ReferralUserCommissionService {

    ReferralUserCommissionVo info(Long userId);

    IPage<AdminReferralUserCommissionListVo> adminPage(ReferralUserCommissionPageBo pageBo);

    void updateInfo(ReferralUserCommissionUpdateBo referralUserCommissionUpdateBo);

    void delete(ReferralUserCommissionInfoBo referralUserCommissionInfoBo);

    /**
     * 订单完成后，保存佣金
     * @param orderId 订单id
     */
    void saveUserCommission(Long orderId);

    ReferralDataVo referralData(Long userId);

    IPage<AdminUserListVo> adminInviteList(Long userId, Integer page, Integer limit);

    IPage<UserEntity> referralUserList(Long userId);

    ReferralTeamOverviewVo referralTeamOverview(Long userId);

    IPage<ReferralTeamMemberVo> referralTeamUserList(Long userId, Integer level, Integer page, Integer limit);

    IPage<ReferralListVo> referralList(Long userId);

    ReferralLawyerVo lawyer();
}
