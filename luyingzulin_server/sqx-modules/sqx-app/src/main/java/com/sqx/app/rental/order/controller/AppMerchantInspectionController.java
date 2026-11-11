package com.sqx.app.rental.order.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.order.domain.bo.RentInspectionSubmitBo;
import com.sqx.app.rental.order.service.RentInspectionService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-验收
 */
@Tag(name = "商户端-验收", description = "商户录入验收结果")
@RestController
@RequestMapping("/app/merchant/inspection")
public class AppMerchantInspectionController {

    @Resource
    private RentInspectionService rentInspectionService;

    @Login
    @Operation(summary = "录入验收", description = "returning/inspecting → completed，计算退押")
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody @Validated RentInspectionSubmitBo submitBo) {
        Long operatorId = UserContextHolder.getAppUserId();
        rentInspectionService.merchantSubmit(operatorId, submitBo);
        return Result.success();
    }
}
