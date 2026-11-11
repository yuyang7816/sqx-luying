package com.sqx.app.referral.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionPageBo;
import com.sqx.app.referral.user.domain.bo.ReferralUserCommissionUpdateBo;
import com.sqx.app.referral.user.domain.vo.AdminReferralUserCommissionListVo;
import com.sqx.app.referral.user.domain.vo.ReferralDataVo;
import com.sqx.app.rental.user.domain.vo.AdminUserListVo;
import com.sqx.app.referral.user.domain.vo.ReferralUserCommissionVo;
import com.sqx.app.referral.user.service.ReferralUserCommissionService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户佣金 前端控制器
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-04 14:44:53
 */
@Tag(name = "管理端-用户佣金", description = "管理端-用户佣金")
@RestController
@RequestMapping("/admin/referral/userCommission")
public class AdminReferralUserCommissionController {

    @Resource
    private ReferralUserCommissionService referralUserCommissionService;

    /**
     * 信息
     */
    @Operation(summary = "信息")
    @PostMapping("info")
    public Result<ReferralUserCommissionVo> info(@RequestParam Long userId) {
        ReferralUserCommissionVo vo = referralUserCommissionService.info(userId);
        return Result.success(vo);
    }

    @Operation(summary = "分页列表")
    @PostMapping("page")
    public Result<IPage<AdminReferralUserCommissionListVo>> page(@RequestBody @Validated ReferralUserCommissionPageBo pageBo) {
        return Result.success(referralUserCommissionService.adminPage(pageBo));
    }

    @Operation(summary = "分销统计（邀请人数、累计佣金）")
    @PostMapping("referralData")
    public Result<ReferralDataVo> referralData(@RequestParam Long userId) {
        return Result.success(referralUserCommissionService.referralData(userId));
    }

    @Operation(summary = "下级用户分页")
    @PostMapping("inviteList")
    public Result<IPage<AdminUserListVo>> inviteList(@RequestParam Long userId,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer limit) {
        return Result.success(referralUserCommissionService.adminInviteList(userId, page, limit));
    }

    /**
     * 更新
     */
    @Operation(summary = "更新")
    @PostMapping("update")
    public Result<Void> update(@RequestBody @Validated ReferralUserCommissionUpdateBo referralUserCommissionUpdateBo) {
        referralUserCommissionService.updateInfo(referralUserCommissionUpdateBo);
        return Result.success();
    }
}
