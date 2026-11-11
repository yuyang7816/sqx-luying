package com.sqx.app.base.platformcs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsConversationPageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsMessagePageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsSendTextBo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsConversationVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsMessageVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsUnreadSummaryVo;
import com.sqx.app.base.platformcs.service.PlatformCsService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "管理端-平台官方客服", description = "平台官方客服 IM 会话与回复")
@RestController
@RequestMapping("/admin/platform-cs")
public class AdminPlatformCsController {

    @Resource
    private PlatformCsService platformCsService;

    @Operation(summary = "会话列表")
    @PostMapping("/conversation/page")
    public Result<IPage<PlatformCsConversationVo>> conversationPage(
            @RequestBody @Validated PlatformCsConversationPageBo pageBo) {
        return Result.success(platformCsService.adminConversationPage(pageBo));
    }

    @Operation(summary = "未读汇总")
    @PostMapping("/unread-summary")
    public Result<PlatformCsUnreadSummaryVo> unreadSummary() {
        return Result.success(platformCsService.adminUnreadSummary());
    }

    @Operation(summary = "打开用户会话")
    @GetMapping("/conversation/open/{userId}")
    public Result<PlatformCsConversationVo> openByUser(@PathVariable Long userId) {
        return Result.success(platformCsService.adminOpenByUser(userId));
    }

    @Operation(summary = "消息列表")
    @PostMapping("/message/page")
    public Result<IPage<PlatformCsMessageVo>> messagePage(@RequestBody @Validated PlatformCsMessagePageBo pageBo) {
        return Result.success(platformCsService.adminMessagePage(pageBo));
    }

    @Operation(summary = "发送回复")
    @PostMapping("/message/send")
    public Result<Void> sendText(@RequestBody @Validated PlatformCsSendTextBo sendBo) {
        Long adminUserId = UserContextHolder.getAppUserId();
        platformCsService.adminSendText(adminUserId, sendBo);
        return Result.success();
    }

    @Operation(summary = "标记会话已读")
    @PostMapping("/conversation/read/{conversationId}")
    public Result<Void> markRead(@PathVariable Long conversationId) {
        platformCsService.adminMarkRead(conversationId);
        return Result.success();
    }
}
