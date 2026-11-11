package com.sqx.app.vip.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.vip.order.domain.bo.VipPurchaseBo;
import com.sqx.app.vip.order.domain.vo.VipPayStatusVo;
import com.sqx.app.vip.order.domain.vo.VipPurchaseVo;
import com.sqx.app.vip.order.service.VipOrderService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "用户端-会员购买", description = "会员套餐购买与支付状态")
@RestController
@RequestMapping("/app/vipOrder")
public class AppVipOrderController {

    @Resource
    private VipOrderService vipOrderService;

    @Login
    @Operation(summary = "创建会员购买订单")
    @PostMapping("/purchase")
    public Result<VipPurchaseVo> purchase(@RequestBody @Validated VipPurchaseBo purchaseBo) {
        return Result.success(vipOrderService.createPurchase(UserContextHolder.getAppUserId(), purchaseBo.getPlanId()));
    }

    @Login
    @Operation(summary = "会员订单支付状态")
    @GetMapping("/pay-status/{orderNo}")
    public Result<VipPayStatusVo> payStatus(@PathVariable("orderNo") String orderNo) {
        return Result.success(vipOrderService.payStatus(UserContextHolder.getAppUserId(), orderNo));
    }
}
