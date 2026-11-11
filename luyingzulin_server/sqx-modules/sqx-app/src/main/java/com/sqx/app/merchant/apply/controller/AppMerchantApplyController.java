package com.sqx.app.merchant.apply.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplySubmitBo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyProgressVo;
import com.sqx.app.merchant.apply.service.MerchantApplyService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 商户端-入驻申请
 */
@Tag(name = "商户端-入驻", description = "提交入驻与查看进度")
@RestController
@RequestMapping("/app/merchant/apply")
public class AppMerchantApplyController {

    @Resource
    private MerchantApplyService merchantApplyService;

    @Login
    @Operation(summary = "提交入驻申请")
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody @Validated MerchantApplySubmitBo submitBo) {
        Long userId = UserContextHolder.getAppUserId();
        merchantApplyService.submit(userId, submitBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "入驻进度")
    @GetMapping("/progress")
    public Result<MerchantApplyProgressVo> progress() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(merchantApplyService.progress(userId));
    }
}
