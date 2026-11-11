package com.sqx.app.vip.plan.controller;

import com.sqx.app.vip.plan.domain.vo.VipPlanVo;
import com.sqx.app.vip.plan.service.VipPlanService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 会员套餐	 前端控制器
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-07 16:51:13
 */
@Tag(name = "用户端-会员套餐	", description = "用户端-会员套餐	")
@RestController
@RequestMapping("/app/vipPlan")
public class AppVipPlanController {

    @Resource
    private VipPlanService vipPlanService;

    /**
     * 列表
     */
    @Operation(summary = "列表")
    @ApiResponse(content = @Content(schema = @Schema(implementation = VipPlanVo.class)))
    @PostMapping("list")
    public Result<List<VipPlanVo>> page() {
        List<VipPlanVo> voList = vipPlanService.list();
        return Result.success(voList);
    }

}
