package com.sqx.app.base.im.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.base.im.domain.bo.ImConversationPageBo;
import com.sqx.app.base.im.domain.bo.ImMessagePageBo;
import com.sqx.app.base.im.domain.bo.ImSendTextBo;
import com.sqx.app.base.im.domain.vo.ImConversationVo;
import com.sqx.app.base.im.domain.vo.ImMessageVo;
import com.sqx.app.base.im.service.ImService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Tag(name = "商户端-IM", description = "商户与用户在线聊天")
@RestController
@RequestMapping("/app/merchant/im")
public class AppMerchantImController {

    @Resource
    private ImService imService;

    @Login
    @Operation(summary = "会话列表")
    @PostMapping("/conversation/page")
    public Result<IPage<ImConversationVo>> conversationPage(@RequestBody @Validated ImConversationPageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(imService.merchantConversationPage(userId, pageBo));
    }

    @Login
    @Operation(summary = "消息列表")
    @PostMapping("/message/page")
    public Result<IPage<ImMessageVo>> messagePage(@RequestBody @Validated ImMessagePageBo pageBo) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(imService.merchantMessagePage(userId, pageBo));
    }

    @Login
    @Operation(summary = "发送文本消息")
    @PostMapping("/message/send")
    public Result<Void> sendText(@RequestBody @Validated ImSendTextBo sendBo) {
        Long userId = UserContextHolder.getAppUserId();
        imService.merchantSendText(userId, sendBo);
        return Result.success();
    }

    @Login
    @Operation(summary = "标记会话已读")
    @PostMapping("/conversation/read/{conversationId}")
    public Result<Void> markRead(@PathVariable Long conversationId) {
        Long userId = UserContextHolder.getAppUserId();
        imService.merchantMarkRead(userId, conversationId);
        return Result.success();
    }

    @Login
    @Operation(summary = "打开与用户的会话")
    @GetMapping("/conversation/open/{targetUserId}")
    public Result<ImConversationVo> openByUser(@PathVariable Long targetUserId) {
        Long userId = UserContextHolder.getAppUserId();
        return Result.success(imService.merchantOpenByUser(userId, targetUserId));
    }
}
