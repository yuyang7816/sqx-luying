package com.sqx.app.merchant.apply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.apply.domain.bo.MerchantPageBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantUpdateStatusBo;
import com.sqx.app.merchant.apply.domain.vo.MerchantDetailVo;
import com.sqx.app.merchant.apply.domain.vo.MerchantListVo;
import com.sqx.app.merchant.apply.service.MerchantAdminService;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositAdjustBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundAuditBo;
import com.sqx.app.merchant.wallet.domain.bo.MerchantSecurityDepositRefundPageBo;
import com.sqx.app.merchant.wallet.domain.vo.AdminMerchantSecurityDepositRefundVo;
import com.sqx.app.merchant.wallet.service.MerchantSecurityDepositService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-商户列表
 */
@Tag(name = "管理端-商户", description = "商户列表与启停")
@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    @Resource
    private MerchantAdminService merchantAdminService;
    @Resource
    private MerchantSecurityDepositService merchantSecurityDepositService;

    @Operation(summary = "商户分页")
    @PostMapping("/page")
    public Result<IPage<MerchantListVo>> page(@RequestBody @Validated MerchantPageBo pageBo) {
        return Result.success(merchantAdminService.page(pageBo));
    }

    @Operation(summary = "商户详情")
    @GetMapping("/detail/{id}")
    public Result<MerchantDetailVo> detail(@PathVariable("id") Long id) {
        return Result.success(merchantAdminService.detail(id));
    }

    @Operation(summary = "启用/冻结商户")
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody @Validated MerchantUpdateStatusBo updateStatusBo) {
        merchantAdminService.updateStatus(updateStatusBo);
        return Result.success();
    }

    @Operation(summary = "调整商户保证金")
    @PostMapping("/deposit/adjust")
    public Result<Void> adjustDeposit(@RequestBody @Validated MerchantSecurityDepositAdjustBo adjustBo) {
        merchantSecurityDepositService.adminAdjust(adjustBo);
        return Result.success();
    }

    @Operation(summary = "商户保证金退还申请分页")
    @PostMapping("/deposit/refund/page")
    public Result<IPage<AdminMerchantSecurityDepositRefundVo>> depositRefundPage(
            @RequestBody @Validated MerchantSecurityDepositRefundPageBo pageBo) {
        return Result.success(merchantSecurityDepositService.adminRefundPage(pageBo));
    }

    @Operation(summary = "审核商户保证金退还")
    @PostMapping("/deposit/refund/audit")
    public Result<Void> depositRefundAudit(@RequestBody @Validated MerchantSecurityDepositRefundAuditBo auditBo) {
        merchantSecurityDepositService.adminAuditRefund(auditBo);
        return Result.success();
    }
}
