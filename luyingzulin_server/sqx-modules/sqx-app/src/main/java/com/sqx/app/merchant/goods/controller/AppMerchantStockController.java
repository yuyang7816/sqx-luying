package com.sqx.app.merchant.goods.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.goods.domain.bo.MerchantStockAdjustBo;
import com.sqx.app.merchant.goods.service.MerchantGoodsService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-库存调整
 */
@Tag(name = "商户端-库存", description = "商户库存入库/出库调整")
@RestController
@RequestMapping("/app/merchant/stock")
public class AppMerchantStockController {

    @Resource
    private MerchantGoodsService merchantGoodsService;

    @Login
    @Operation(summary = "库存调整", description = "adjustType: in入库 / out出库")
    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody @Validated MerchantStockAdjustBo adjustBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantGoodsService.adjustStock(userId, adjustBo);
        return Result.success();
    }
}
