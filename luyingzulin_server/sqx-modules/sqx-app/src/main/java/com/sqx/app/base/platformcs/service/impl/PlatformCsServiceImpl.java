package com.sqx.app.base.platformcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.notification.core.enums.NotificationChannel;
import com.sqx.app.base.notification.core.model.NotificationRequest;
import com.sqx.app.base.notification.core.service.INotificationService;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsConversationPageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsMessagePageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsSendTextBo;
import com.sqx.app.base.platformcs.domain.entity.PlatformCsConversation;
import com.sqx.app.base.platformcs.domain.entity.PlatformCsMessage;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsConversationVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsMessageVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsUnreadSummaryVo;
import com.sqx.app.base.platformcs.enums.PlatformCsSenderRole;
import com.sqx.app.base.platformcs.mapper.PlatformCsConversationMapper;
import com.sqx.app.base.platformcs.mapper.PlatformCsMessageMapper;
import com.sqx.app.base.platformcs.service.PlatformCsService;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PlatformCsServiceImpl implements PlatformCsService {

    private static final int PREVIEW_MAX_LEN = 80;
    private static final int TYPE_PLATFORM_CS = 4;
    private static final String PEER_NAME = "官方客服";
	private static final String PEER_AVATAR = "";

    @Resource
    private PlatformCsConversationMapper conversationMapper;
    @Resource
    private PlatformCsMessageMapper messageMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private INotificationService notificationService;

    @Override
    public PlatformCsConversationVo userOpen(Long userId) {
        PlatformCsConversation conversation = getOrCreateConversation(userId);
        return toUserVo(conversation);
    }

    @Override
    public IPage<PlatformCsMessageVo> userMessagePage(Long userId, PlatformCsMessagePageBo pageBo) {
        PlatformCsConversation conversation = requireUserConversation(userId, pageBo.getConversationId());
        return messagePage(conversation, pageBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userSendText(Long userId, PlatformCsSendTextBo sendBo) {
        PlatformCsConversation conversation = requireUserConversation(userId, sendBo.getConversationId());
        String content = sendBo.getContent().trim();
        insertMessage(conversation.getId(), PlatformCsSenderRole.USER.getCode(), userId, null, content);
        bumpConversation(conversation.getId(), content, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userMarkRead(Long userId, Long conversationId) {
        PlatformCsConversation conversation = requireUserConversation(userId, conversationId);
        conversationMapper.update(null, new LambdaUpdateWrapper<PlatformCsConversation>()
                .eq(PlatformCsConversation::getId, conversation.getId())
                .set(PlatformCsConversation::getUserUnread, 0));
        markMessagesRead(conversation.getId(), PlatformCsSenderRole.ADMIN.getCode());
    }

    @Override
    public int countUserUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        PlatformCsConversation conversation = conversationMapper.selectOne(new LambdaQueryWrapper<PlatformCsConversation>()
                .eq(PlatformCsConversation::getUserId, userId)
                .last("LIMIT 1"));
        return conversation == null || conversation.getUserUnread() == null ? 0 : conversation.getUserUnread();
    }

    @Override
    public IPage<PlatformCsConversationVo> adminConversationPage(PlatformCsConversationPageBo pageBo) {
        Page<PlatformCsConversation> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<PlatformCsConversation> wrapper = new LambdaQueryWrapper<PlatformCsConversation>()
                .orderByDesc(PlatformCsConversation::getLastMsgTime)
                .orderByDesc(PlatformCsConversation::getId);
        if (Boolean.TRUE.equals(pageBo.getUnreadOnly())) {
            wrapper.gt(PlatformCsConversation::getAdminUnread, 0);
        }
        IPage<PlatformCsConversation> convPage = conversationMapper.selectPage(page, wrapper);
        List<PlatformCsConversationVo> voList = toAdminVoList(convPage.getRecords());
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            String keyword = pageBo.getKeyword().trim();
            voList = voList.stream()
                    .filter(item -> StringUtils.containsIgnoreCase(StringUtils.defaultString(item.getUserName()), keyword)
                            || StringUtils.contains(StringUtils.defaultString(item.getUserPhone()), keyword))
                    .collect(Collectors.toList());
        }
        return PageUtil.copyPage(convPage, voList);
    }

    @Override
    public PlatformCsConversationVo adminOpenByUser(Long userId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        PlatformCsConversation conversation = getOrCreateConversation(userId);
        return toAdminVo(conversation);
    }

    @Override
    public IPage<PlatformCsMessageVo> adminMessagePage(PlatformCsMessagePageBo pageBo) {
        PlatformCsConversation conversation = requireConversation(pageBo.getConversationId());
        return messagePage(conversation, pageBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminSendText(Long adminUserId, PlatformCsSendTextBo sendBo) {
        PlatformCsConversation conversation = requireConversation(sendBo.getConversationId());
        String content = sendBo.getContent().trim();
        insertMessage(conversation.getId(), PlatformCsSenderRole.ADMIN.getCode(), null, adminUserId, content);
        bumpConversation(conversation.getId(), content, true);
        notifyUserReply(conversation.getUserId(), content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminMarkRead(Long conversationId) {
        PlatformCsConversation conversation = requireConversation(conversationId);
        conversationMapper.update(null, new LambdaUpdateWrapper<PlatformCsConversation>()
                .eq(PlatformCsConversation::getId, conversation.getId())
                .set(PlatformCsConversation::getAdminUnread, 0));
        markMessagesRead(conversation.getId(), PlatformCsSenderRole.USER.getCode());
    }

    @Override
    public PlatformCsUnreadSummaryVo adminUnreadSummary() {
        List<PlatformCsConversation> conversations = conversationMapper.selectList(new LambdaQueryWrapper<PlatformCsConversation>()
                .gt(PlatformCsConversation::getAdminUnread, 0)
                .select(PlatformCsConversation::getAdminUnread));
        PlatformCsUnreadSummaryVo vo = new PlatformCsUnreadSummaryVo();
        if (CollectionUtils.isEmpty(conversations)) {
            vo.setUnreadConversations(0);
            vo.setUnreadMessages(0);
            return vo;
        }
        int total = 0;
        for (PlatformCsConversation conversation : conversations) {
            if (conversation.getAdminUnread() != null && conversation.getAdminUnread() > 0) {
                total += conversation.getAdminUnread();
            }
        }
        vo.setUnreadConversations(conversations.size());
        vo.setUnreadMessages(total);
        return vo;
    }

    private IPage<PlatformCsMessageVo> messagePage(PlatformCsConversation conversation, PlatformCsMessagePageBo pageBo) {
        Page<PlatformCsMessage> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<PlatformCsMessage> wrapper = new LambdaQueryWrapper<PlatformCsMessage>()
                .eq(PlatformCsMessage::getConversationId, conversation.getId())
                .orderByDesc(PlatformCsMessage::getCreateTime)
                .orderByDesc(PlatformCsMessage::getId);
        IPage<PlatformCsMessage> msgPage = messageMapper.selectPage(page, wrapper);
        List<PlatformCsMessageVo> voList = msgPage.getRecords().stream()
                .map(this::toMessageVo)
                .collect(Collectors.toList());
        Collections.reverse(voList);
        return PageUtil.copyPage(msgPage, voList);
    }

    private PlatformCsConversation getOrCreateConversation(Long userId) {
        PlatformCsConversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<PlatformCsConversation>()
                .eq(PlatformCsConversation::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        PlatformCsConversation conversation = new PlatformCsConversation();
        conversation.setUserId(userId);
        conversation.setUserUnread(0);
        conversation.setAdminUnread(0);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private void insertMessage(Long conversationId, String senderRole, Long senderUserId,
                               Long senderAdminId, String content) {
        PlatformCsMessage message = new PlatformCsMessage();
        message.setConversationId(conversationId);
        message.setSenderRole(senderRole);
        message.setSenderUserId(senderUserId);
        message.setSenderAdminId(senderAdminId);
        message.setMsgType("text");
        message.setContent(content);
        message.setIsRead(0);
        messageMapper.insert(message);
    }

    private void bumpConversation(Long conversationId, String preview, boolean adminSent) {
        String text = StringUtils.defaultString(preview, "");
        if (text.length() > PREVIEW_MAX_LEN) {
            text = text.substring(0, PREVIEW_MAX_LEN) + "...";
        }
        LambdaUpdateWrapper<PlatformCsConversation> wrapper = new LambdaUpdateWrapper<PlatformCsConversation>()
                .eq(PlatformCsConversation::getId, conversationId)
                .set(PlatformCsConversation::getLastMsgTime, LocalDateTime.now())
                .set(PlatformCsConversation::getLastMsgPreview, text);
        if (adminSent) {
            wrapper.setSql("user_unread = user_unread + 1");
        } else {
            wrapper.setSql("admin_unread = admin_unread + 1");
        }
        conversationMapper.update(null, wrapper);
    }

    private void markMessagesRead(Long conversationId, String senderRole) {
        messageMapper.update(null, new LambdaUpdateWrapper<PlatformCsMessage>()
                .eq(PlatformCsMessage::getConversationId, conversationId)
                .eq(PlatformCsMessage::getSenderRole, senderRole)
                .eq(PlatformCsMessage::getIsRead, 0)
                .set(PlatformCsMessage::getIsRead, 1));
    }

    private PlatformCsConversation requireUserConversation(Long userId, Long conversationId) {
        PlatformCsConversation conversation = requireConversation(conversationId);
        if (!userId.equals(conversation.getUserId())) {
            throw new BizException("会话不存在或无权访问");
        }
        return conversation;
    }

    private PlatformCsConversation requireConversation(Long conversationId) {
        PlatformCsConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BizException("会话不存在");
        }
        return conversation;
    }

    private PlatformCsConversationVo toUserVo(PlatformCsConversation conversation) {
        PlatformCsConversationVo vo = new PlatformCsConversationVo();
        BeanUtils.copyProperties(conversation, vo);
        vo.setPeerName(PEER_NAME);
        vo.setPeerAvatar(PEER_AVATAR);
        vo.setUnreadCount(conversation.getUserUnread() == null ? 0 : conversation.getUserUnread());
        return vo;
    }

    private List<PlatformCsConversationVo> toAdminVoList(List<PlatformCsConversation> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Map<Long, UserEntity> userMap = loadUserMap(records.stream()
                .map(PlatformCsConversation::getUserId).collect(Collectors.toList()));
        List<PlatformCsConversationVo> list = new ArrayList<>();
        for (PlatformCsConversation item : records) {
            list.add(toAdminVo(item, userMap.get(item.getUserId())));
        }
        return list;
    }

    private PlatformCsConversationVo toAdminVo(PlatformCsConversation conversation) {
        UserEntity user = userMapper.selectById(conversation.getUserId());
        return toAdminVo(conversation, user);
    }

    private PlatformCsConversationVo toAdminVo(PlatformCsConversation conversation, UserEntity user) {
        PlatformCsConversationVo vo = new PlatformCsConversationVo();
        BeanUtils.copyProperties(conversation, vo);
        vo.setAdminUnread(conversation.getAdminUnread() == null ? 0 : conversation.getAdminUnread());
        vo.setUnreadCount(vo.getAdminUnread());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setUserPhone(user.getPhone());
        }
        return vo;
    }

    private PlatformCsMessageVo toMessageVo(PlatformCsMessage message) {
        PlatformCsMessageVo vo = new PlatformCsMessageVo();
        BeanUtils.copyProperties(message, vo);
        vo.setIsRead(message.getIsRead() != null && message.getIsRead() == 1);
        return vo;
    }

    private Map<Long, UserEntity> loadUserMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(UserEntity::getUserId, item -> item, (a, b) -> a));
    }

    private void notifyUserReply(Long userId, String preview) {
        if (userId == null) {
            return;
        }
        String text = StringUtils.defaultString(preview, "");
        if (text.length() > PREVIEW_MAX_LEN) {
            text = text.substring(0, PREVIEW_MAX_LEN) + "...";
        }
        try {
            NotificationRequest request = new NotificationRequest();
            request.setUserId(userId);
            request.setTitle("官方客服回复了您的消息");
            request.setContent(text);
            request.setType(TYPE_PLATFORM_CS);
            request.setUrl("/packageUser/platform-im-chat");
            notificationService.send(request, NotificationChannel.INSTATION.code());
        } catch (Exception e) {
            log.warn("平台客服通知发送失败 userId={}: {}", userId, e.getMessage());
        }
    }
}
