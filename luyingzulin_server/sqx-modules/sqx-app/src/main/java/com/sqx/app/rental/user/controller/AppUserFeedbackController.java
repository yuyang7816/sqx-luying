package com.sqx.app.rental.user.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.rental.user.domain.bo.UserFeedbackSubmitBo;
import com.sqx.app.rental.user.service.UserFeedbackService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "用户端-意见反馈", description = "提交反馈")
@RestController
@RequestMapping("/app/user/feedback")
public class AppUserFeedbackController {

    @Resource
    private UserFeedbackService userFeedbackService;

    @Login
    @Operation(summary = "提交反馈")
    @PostMapping("/submit")
    public Result<Void> submit(@RequestBody @Validated UserFeedbackSubmitBo submitBo) {
        userFeedbackService.submit(UserContextHolder.getAppUserId(), submitBo);
        return Result.success();
    }
}
