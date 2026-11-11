package com.sqx.app.rental.store.controller;

import com.sqx.app.rental.store.domain.vo.RentPickupStoreVo;
import com.sqx.app.rental.store.service.RentPickupStoreService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * C 端取货门店/自提点
 */
@Tag(name = "用户端-取货门店", description = "下单页门店/驿站选择")
@RestController
@RequestMapping("/app/rental/store")
public class AppRentPickupStoreController {

    @Resource
    private RentPickupStoreService rentPickupStoreService;

    @Operation(summary = "取货点位列表", description = "pickupType=store 门店自提，pickup_point 驿站自提")
    @GetMapping("/list")
    public Result<List<RentPickupStoreVo>> list(
            @RequestParam(value = "pickupType", required = false) String pickupType,
            @RequestParam(value = "merchantId", required = false) Long merchantId) {
        return Result.success(rentPickupStoreService.list(pickupType, merchantId));
    }
}
