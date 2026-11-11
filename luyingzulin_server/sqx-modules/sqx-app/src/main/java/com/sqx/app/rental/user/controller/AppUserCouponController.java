package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.CouponClaimBo;
import com.sqx.app.rental.user.domain.bo.UserCouponConsumeBo;
import com.sqx.app.rental.user.domain.bo.UserCouponPageBo;
import com.sqx.app.rental.user.domain.bo.UserCouponUsableBo;
import com.sqx.app.rental.user.domain.vo.ClaimableCouponVo;
import com.sqx.app.rental.user.domain.vo.UserCouponVo;
import com.sqx.app.rental.user.service.UserCouponService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Tag(name = "用户端-优惠券", description = "我的优惠券、可用券与核销")
@RestController
@RequestMapping("/app/user/coupon")
public class AppUserCouponController {

    @Resource
    private UserCouponService userCouponService;

    @Login
    @Operation(summary = "我的优惠券分页")
    @PostMapping("/page")
    public Result<IPage<UserCouponVo>> page(@RequestBody @Validated UserCouponPageBo pageBo) {
        return Result.success(userCouponService.page(UserContextHolder.getAppUserId(), pageBo));
    }

    @Login
    @Operation(summary = "可领取优惠券列表")
    @GetMapping("/claimable")
    public Result<List<ClaimableCouponVo>> claimable() {
        return Result.success(userCouponService.listClaimable(UserContextHolder.getAppUserId()));
    }

    @Login
    @Operation(summary = "领取优惠券")
    @PostMapping("/claim")
    public Result<Void> claim(@RequestBody @Validated CouponClaimBo claimBo) {
        userCouponService.claim(UserContextHolder.getAppUserId(), claimBo.getTemplateId());
        return Result.success();
    }

    @Login
    @Operation(summary = "下单可用优惠券")
    @GetMapping("/usable")
    public Result<List<UserCouponVo>> usable(UserCouponUsableBo usableBo) {
        return Result.success(userCouponService.listUsable(UserContextHolder.getAppUserId(), usableBo));
    }

    @Login
    @Operation(summary = "核销优惠券")
    @PostMapping("/consume")
    public Result<Void> consume(@RequestBody @Validated UserCouponConsumeBo consumeBo) {
        userCouponService.consume(UserContextHolder.getAppUserId(), consumeBo);
        return Result.success();
    }
}
