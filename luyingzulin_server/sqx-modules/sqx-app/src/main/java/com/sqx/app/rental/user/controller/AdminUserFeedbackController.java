package com.sqx.app.rental.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.UserFeedbackPageBo;
import com.sqx.app.rental.user.domain.bo.UserFeedbackProcessBo;
import com.sqx.app.rental.user.domain.vo.UserFeedbackVo;
import com.sqx.app.rental.user.service.UserFeedbackService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@Tag(name = "管理端-用户反馈", description = "反馈列表、详情与处理")
@RestController
@RequestMapping("/admin/user/feedback")
public class AdminUserFeedbackController {

    @Resource
    private UserFeedbackService userFeedbackService;

    @Operation(summary = "反馈分页")
    @PostMapping("/page")
    public Result<IPage<UserFeedbackVo>> page(@RequestBody @Validated UserFeedbackPageBo pageBo) {
        return Result.success(userFeedbackService.adminPage(pageBo));
    }

    @Operation(summary = "反馈详情")
    @GetMapping("/info")
    public Result<UserFeedbackVo> info(@NotNull @RequestParam("id") Long id) {
        return Result.success(userFeedbackService.adminInfo(id));
    }

    @Operation(summary = "处理反馈")
    @PostMapping("/process")
    public Result<Void> process(@RequestBody @Validated UserFeedbackProcessBo processBo) {
        userFeedbackService.process(processBo);
        return Result.success();
    }
}
