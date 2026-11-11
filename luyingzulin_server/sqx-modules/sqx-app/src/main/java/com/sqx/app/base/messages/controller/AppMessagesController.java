package com.sqx.app.base.messages.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.app.base.messages.domain.bo.MessagesInfoBo;
import com.sqx.app.base.messages.domain.bo.MessagesPageBo;
import com.sqx.app.base.messages.domain.bo.MessagesSaveBo;
import com.sqx.app.base.messages.domain.bo.MessagesUpdateBo;
import com.sqx.app.base.messages.domain.vo.MessagesUnreadSummaryVo;
import com.sqx.app.base.messages.domain.vo.MessagesSubscribeTemplateVo;
import com.sqx.app.base.messages.domain.vo.MessagesVo;
import com.sqx.app.base.messages.service.MessagesService;
import com.sqx.app.base.im.service.ImService;
import com.sqx.app.base.platformcs.service.PlatformCsService;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 消息信息表 前端控制器
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-15 16:28:04
 */
@Tag(name = "用户端-消息信息表", description = "用户端-消息信息表")
@RestController
@RequestMapping("/app/messages")
public class AppMessagesController {

    @Resource
    private MessagesService messagesService;
    @Resource
    private ImService imService;
    @Resource
    private PlatformCsService platformCsService;

    /**
     * 保存
     */
    @Operation(summary = "保存")
    @PostMapping("save")
    public Result<Void> save(@RequestBody @Validated MessagesSaveBo messagesSaveBo) {
        messagesService.saveInfo(messagesSaveBo);
        return Result.success();
    }

    /**
     * 列表
     */
    @Login
    @Operation(summary = "列表")
    @ApiResponse(content = @Content(schema = @Schema(implementation = MessagesVo.class)))
    @PostMapping("page")
    public Result<IPage<MessagesVo>> page(@RequestAttribute Long userId,
                                            @RequestBody @Validated MessagesPageBo messagesPageBo) {
        messagesPageBo.setUserId(userId);
        IPage<MessagesVo> voIPage = messagesService.pageInfo(messagesPageBo);
        return Result.success(voIPage);
    }

    /**
     * 信息
     */
    @Login
    @Operation(summary = "信息")
    @ApiResponse(content = @Content(schema = @Schema(implementation = MessagesVo.class)))
    @PostMapping("info")
    public Result<MessagesVo> info(@RequestAttribute Long userId,
                                   @RequestBody @Validated MessagesInfoBo messagesInfoBo) {
        MessagesVo vo = messagesService.info(messagesInfoBo);
        if (vo.getUserId() != null && !userId.equals(vo.getUserId())) {
            return Result.failed("无权查看该消息");
        }
        return Result.success(vo);
    }

    /**
     * 标记已读
     */
    @Login
    @Operation(summary = "标记已读")
    @PostMapping("read")
    public Result<Void> read(@RequestAttribute Long userId,
                             @RequestBody @Validated MessagesInfoBo messagesInfoBo) {
        messagesService.markRead(userId, messagesInfoBo.getId());
        return Result.success();
    }

    /**
     * 更新
     */
    @Login
    @Operation(summary = "更新")
    @PostMapping("update")
    public Result<Void> update(@RequestAttribute Long userId,
                               @RequestBody @Validated MessagesUpdateBo messagesUpdateBo) {
        messagesService.markRead(userId, messagesUpdateBo.getId());
        return Result.success();
    }

    /**
     * 未读汇总（系统通知 + IM）
     */
    @Login
    @Operation(summary = "未读汇总")
    @PostMapping("unread-summary")
    public Result<MessagesUnreadSummaryVo> unreadSummary(@RequestAttribute Long userId) {
        int imUnread = imService.countUserUnread(userId) + platformCsService.countUserUnread(userId);
        return Result.success(messagesService.unreadSummary(userId, imUnread));
    }

    /**
     * 小程序订阅消息模板
     */
    @Login
    @Operation(summary = "订阅消息模板")
    @PostMapping("subscribe-templates")
    public Result<MessagesSubscribeTemplateVo> subscribeTemplates() {
        return Result.success(messagesService.subscribeTemplates());
    }

    /**
     * 删除
     */
    @Operation(summary = "删除")
    @PostMapping("delete")
    public Result<Void> delete(@RequestBody @Validated MessagesInfoBo messagesInfoBo) {
        messagesService.delete(messagesInfoBo);
        return Result.success();
    }
}
