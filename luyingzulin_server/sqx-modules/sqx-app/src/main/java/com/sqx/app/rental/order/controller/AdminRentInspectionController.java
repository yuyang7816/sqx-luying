package com.sqx.app.rental.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.order.domain.bo.RentInspectionAuditBo;
import com.sqx.app.rental.order.domain.bo.RentInspectionPageBo;
import com.sqx.app.rental.order.domain.vo.RentInspectionDetailVo;
import com.sqx.app.rental.order.domain.vo.RentInspectionListVo;
import com.sqx.app.rental.order.service.RentInspectionService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 管理端-验收记录
 */
@Tag(name = "管理端-验收记录", description = "验收列表、详情与复核")
@RestController
@RequestMapping("/admin/rental/inspection")
public class AdminRentInspectionController {

    @Resource
    private RentInspectionService rentInspectionService;

    @Operation(summary = "验收分页", description = "支持订单号、复核状态、设备状况筛选")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentInspectionListVo.class)))
    @PostMapping("/page")
    public Result<IPage<RentInspectionListVo>> page(@RequestBody @Validated RentInspectionPageBo pageBo) {
        return Result.success(rentInspectionService.adminPage(pageBo));
    }

    @Operation(summary = "验收详情", description = "含用户、扣款、退押、图片")
    @ApiResponse(content = @Content(schema = @Schema(implementation = RentInspectionDetailVo.class)))
    @GetMapping("/detail/{id}")
    public Result<RentInspectionDetailVo> detail(@PathVariable("id") Long id) {
        return Result.success(rentInspectionService.adminDetail(id));
    }

    @Operation(summary = "验收复核", description = "运营对商户验收结果复核")
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody @Validated RentInspectionAuditBo auditBo) {
        rentInspectionService.adminAudit(auditBo);
        return Result.success();
    }
}
