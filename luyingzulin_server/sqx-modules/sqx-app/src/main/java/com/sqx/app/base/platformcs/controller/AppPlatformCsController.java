package com.sqx.app.base.platformcs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsMessagePageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsSendTextBo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsConversationVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsMessageVo;
import com.sqx.app.base.platformcs.service.PlatformCsService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "用户端-平台官方客服", description = "平台官方在线客服 IM")
@RestController
@RequestMapping("/app/platform-cs")
public class AppPlatformCsController {

    @Resource
    private PlatformCsService platformCsService;

    @Login
    @Operation(summary = "打开/创建官方客服会话")
    @GetMapping("/conversation/open")
    public Result<PlatformCsConversationVo> openConversation() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(platformCsService.userOpen(userId));
    }

    @Login
    @Operation(summary = "消息列表")
    @PostMapping("/message/page")
    public Result<IPage<PlatformCsMessageVo>> messagePage(@RequestBody @Validated PlatformCsMessagePageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(platformCsService.userMessagePage(userId, pageBo));
    }

    @Login
    @Operation(summary = "发送文本消息")
    @PostMapping("/message/send")
    public Result<Void> sendText(@RequestBody @Validated PlatformCsSendTextBo sendBo) {
        Long userId = UserContextHolder.getAppUserId();
        platformCsService.userSendText(userId, sendBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "用户未读消息数")
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(platformCsService.countUserUnread(userId));
    }

    @Login
    @Operation(summary = "标记会话已读")
    @PostMapping("/conversation/read/{conversationId}")
    public Result<Void> markRead(@PathVariable Long conversationId) {
        Long userId = UserContextHolder.getAppUserId();
        platformCsService.userMarkRead(userId, conversationId);
        return Result.success();
    }
}
