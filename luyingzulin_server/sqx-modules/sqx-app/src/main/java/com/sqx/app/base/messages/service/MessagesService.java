package com.sqx.app.base.messages.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.messages.domain.bo.MessagesInfoBo;
import com.sqx.app.base.messages.domain.bo.MessagesPageBo;
import com.sqx.app.base.messages.domain.bo.MessagesSaveBo;
import com.sqx.app.base.messages.domain.bo.MessagesUpdateBo;
import com.sqx.app.base.messages.domain.vo.MessagesUnreadSummaryVo;
import com.sqx.app.base.messages.domain.vo.MessagesSubscribeTemplateVo;
import com.sqx.app.base.messages.domain.vo.MessagesVo;

/**
 * 消息信息表 服务类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-15 16:28:04
 */
public interface MessagesService {

    void saveInfo(MessagesSaveBo messagesSaveBo);

    IPage<MessagesVo> pageInfo(MessagesPageBo messagesPageBo);

    MessagesVo info(MessagesInfoBo messagesInfoBo);

    void markRead(Long userId, Long messageId);

    void updateInfo(MessagesUpdateBo messagesUpdateBo);

    void delete(MessagesInfoBo messagesInfoBo);

    int countUnread(Long userId, Integer type);

    int countSystemNoticeUnread(Long userId);

    MessagesUnreadSummaryVo unreadSummary(Long userId, int imUnread);

    MessagesSubscribeTemplateVo subscribeTemplates();
}
