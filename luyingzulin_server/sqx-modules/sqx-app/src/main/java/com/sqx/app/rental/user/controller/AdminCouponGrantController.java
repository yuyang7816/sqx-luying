package com.sqx.app.rental.user.controller;

import com.sqx.app.rental.user.domain.bo.CouponGrantBo;
import com.sqx.app.rental.user.service.CouponTemplateService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Map;

@Tag(name = "管理端-优惠券发放", description = "向用户发放优惠券")
@RestController
@RequestMapping("/admin/coupon")
public class AdminCouponGrantController {

    @Resource
    private CouponTemplateService couponTemplateService;

    @Operation(summary = "发放优惠券")
    @PostMapping("/grant")
    public Result<Map<String, Integer>> grant(@RequestBody @Validated CouponGrantBo grantBo) {
        int count = couponTemplateService.grant(grantBo);
        return Result.success(Collections.singletonMap("grantCount", count));
    }
}
