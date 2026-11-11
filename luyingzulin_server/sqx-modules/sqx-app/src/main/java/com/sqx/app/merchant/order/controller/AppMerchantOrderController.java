package com.sqx.app.merchant.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPageBo;
import com.sqx.app.merchant.order.domain.bo.MerchantOrderPickupBo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderContactVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderDetailVo;
import com.sqx.app.merchant.order.domain.vo.MerchantOrderListVo;
import com.sqx.app.merchant.order.service.MerchantOrderService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-订单处理
 */
@Tag(name = "商户端-订单", description = "商户订单列表、备货、核销")
@RestController
@RequestMapping("/app/merchant/order")
public class AppMerchantOrderController {

    @Resource
    private MerchantOrderService merchantOrderService;

    @Login
    @Operation(summary = "订单分页", description = "Tab: all/pending/running/review/completed")
    @PostMapping("/page")
    public Result<IPage<MerchantOrderListVo>> page(@RequestBody @Validated MerchantOrderPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantOrderService.page(userId, pageBo));
    }

    @Login
    @Operation(summary = "订单详情", description = "id 支持数字ID或 LINK-{rentOrderId}")
    @GetMapping("/detail/{id}")
    public Result<MerchantOrderDetailVo> detail(@PathVariable("id") String id) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantOrderService.detail(userId, id));
    }

    @Login
    @Operation(summary = "确认备货")
    @PostMapping("/prepare/{id}")
    public Result<Void> prepare(@PathVariable("id") String id) {
        Long userId = UserContextHolder.getAppUserId();
        merchantOrderService.prepare(userId, id);
        return Result.success();
    }

    @Login
    @Operation(summary = "确认取货/核销", description = "校验提货码后回写用户订单为 renting")
    @PostMapping("/pickup/{id}")
    public Result<Void> pickup(@PathVariable("id") String id,
                               @RequestBody @Validated MerchantOrderPickupBo pickupBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantOrderService.pickup(userId, id, pickupBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "按提货码核销", description = "扫码或输入提货码直接核销，返回订单详情")
    @PostMapping("/pickup-by-code")
    public Result<MerchantOrderDetailVo> pickupByCode(@RequestBody @Validated MerchantOrderPickupBo pickupBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantOrderService.pickupByCode(userId, pickupBo));
    }

    @Login
    @Operation(summary = "联系用户")
    @GetMapping("/contact/{id}")
    public Result<MerchantOrderContactVo> contact(@PathVariable("id") String id) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantOrderService.contact(userId, id));
    }
}
