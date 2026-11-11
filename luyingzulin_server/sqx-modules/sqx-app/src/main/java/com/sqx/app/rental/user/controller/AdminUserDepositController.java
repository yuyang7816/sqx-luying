package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositAdjustBo;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositLogPageBo;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositPageBo;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositRefundAuditBo;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositRefundBo;
import com.sqx.app.rental.user.domain.bo.AdminUserDepositRefundPageBo;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositRefundVo;
import com.sqx.app.rental.user.domain.vo.AdminUserDepositVo;
import com.sqx.app.rental.user.domain.vo.UserDepositLogVo;
import com.sqx.app.rental.user.service.UserDepositService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "管理端-用户押金", description = "押金账户列表与人工退押")
@RestController
@RequestMapping("/admin/user/deposit")
public class AdminUserDepositController {

    @Resource
    private UserDepositService userDepositService;

    @Operation(summary = "押金账户分页")
    @PostMapping("/page")
    public Result<IPage<AdminUserDepositVo>> page(@RequestBody @Validated AdminUserDepositPageBo pageBo) {
        return Result.success(userDepositService.adminPage(pageBo));
    }

    @Operation(summary = "人工退押")
    @PostMapping("/refund")
    public Result<Void> refund(@RequestBody @Validated AdminUserDepositRefundBo refundBo) {
        userDepositService.adminRefund(refundBo);
        return Result.success();
    }

    @Operation(summary = "人工调押")
    @PostMapping("/adjust")
    public Result<Void> adjust(@RequestBody @Validated AdminUserDepositAdjustBo adjustBo) {
        userDepositService.adminAdjust(adjustBo);
        return Result.success();
    }

    @Operation(summary = "押金流水分页")
    @PostMapping("/log/page")
    public Result<IPage<UserDepositLogVo>> logPage(@RequestBody @Validated AdminUserDepositLogPageBo pageBo) {
        return Result.success(userDepositService.logPage(pageBo.getUserId(), pageBo));
    }

    @Operation(summary = "用户退押申请分页")
    @PostMapping("/refund/page")
    public Result<IPage<AdminUserDepositRefundVo>> refundPage(
            @RequestBody @Validated AdminUserDepositRefundPageBo pageBo) {
        return Result.success(userDepositService.adminRefundPage(pageBo));
    }

    @Operation(summary = "审核用户退押申请")
    @PostMapping("/refund/audit")
    public Result<Void> refundAudit(@RequestBody @Validated AdminUserDepositRefundAuditBo auditBo) {
        userDepositService.adminAuditRefund(auditBo);
        return Result.success();
    }
}
