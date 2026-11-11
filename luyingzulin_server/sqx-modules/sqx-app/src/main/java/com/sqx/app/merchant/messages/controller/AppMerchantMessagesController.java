package com.sqx.app.merchant.messages.controller;

import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.base.messages.domain.vo.MessagesUnreadSummaryVo;
import com.sqx.app.base.messages.service.MessagesService;
import com.sqx.app.base.im.service.ImService;
import com.sqx.common.core.domain.Result;
import com.sqx.common.shiro.utils.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Tag(name = "商户端-消息", description = "商户消息未读汇总")
@RestController
@RequestMapping("/app/merchant/messages")
public class AppMerchantMessagesController {

    @Resource
    private MessagesService messagesService;
    @Resource
    private ImService imService;

    @Login
    @Operation(summary = "未读汇总")
    @PostMapping("unread-summary")
    public Result<MessagesUnreadSummaryVo> unreadSummary() {
        Long userId = UserContextHolder.getAppUserId();
        MessagesUnreadSummaryVo vo = new MessagesUnreadSummaryVo();
        int systemUnread = messagesService.countSystemNoticeUnread(userId);
        int imUnread = imService.countMerchantUnread(userId);
        vo.setSystemUnread(systemUnread);
        vo.setImUnread(imUnread);
        vo.setTotalUnread(systemUnread + imUnread);
        return Result.success(vo);
    }
}
