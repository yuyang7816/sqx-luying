package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.CouponGrantBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplatePageBo;
import com.sqx.app.rental.user.domain.bo.CouponTemplateSaveBo;
import com.sqx.app.rental.user.domain.vo.CouponTemplateVo;
import com.sqx.app.rental.user.service.CouponTemplateService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@Tag(name = "管理端-优惠券模板", description = "券模板 CRUD 与发放")
@RestController
@RequestMapping("/admin/coupon/template")
public class AdminCouponTemplateController {

    @Resource
    private CouponTemplateService couponTemplateService;

    @Operation(summary = "模板分页")
    @PostMapping("/page")
    public Result<IPage<CouponTemplateVo>> page(@RequestBody @Validated CouponTemplatePageBo pageBo) {
        return Result.success(couponTemplateService.page(pageBo));
    }

    @Operation(summary = "模板详情")
    @GetMapping("/info")
    public Result<CouponTemplateVo> info(@NotNull @RequestParam("id") Long id) {
        return Result.success(couponTemplateService.info(id));
    }

    @Operation(summary = "新增模板")
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Validated CouponTemplateSaveBo saveBo) {
        couponTemplateService.save(saveBo);
        return Result.success();
    }

    @Operation(summary = "更新模板")
    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Validated CouponTemplateSaveBo saveBo) {
        couponTemplateService.update(saveBo);
        return Result.success();
    }

    @Operation(summary = "删除模板")
    @PostMapping("/delete")
    public Result<Void> delete(@RequestBody CouponTemplateSaveBo deleteBo) {
        couponTemplateService.delete(deleteBo.getId());
        return Result.success();
    }
}
