package com.sqx.app.merchant.apply.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyAuditBo;
import com.sqx.app.merchant.apply.domain.bo.MerchantApplyPageBo;
import com.sqx.app.merchant.apply.domain.vo.MerchantApplyListVo;
import com.sqx.app.merchant.apply.service.MerchantApplyService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-商户入驻审核
 */
@Tag(name = "管理端-商户入驻", description = "入驻申请列表与审核")
@RestController
@RequestMapping("/admin/merchant/apply")
public class AdminMerchantApplyController {

    @Resource
    private MerchantApplyService merchantApplyService;

    @Operation(summary = "入驻申请分页")
    @PostMapping("/page")
    public Result<IPage<MerchantApplyListVo>> page(@RequestBody @Validated MerchantApplyPageBo pageBo) {
        return Result.success(merchantApplyService.adminPage(pageBo));
    }

    @Operation(summary = "审核入驻申请", description = "approved通过 / rejected驳回")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody @Validated MerchantApplyAuditBo auditBo) {
        merchantApplyService.audit(0L, auditBo);
        return Result.success();
    }
}
