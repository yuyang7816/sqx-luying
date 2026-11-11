package com.sqx.app.base.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.im.domain.bo.ImConversationPageBo;
import com.sqx.app.base.im.domain.bo.ImMessagePageBo;
import com.sqx.app.base.im.domain.bo.ImSendTextBo;
import com.sqx.app.base.im.domain.entity.ImConversation;
import com.sqx.app.base.im.domain.entity.ImMessage;
import com.sqx.app.base.im.domain.vo.ImConversationVo;
import com.sqx.app.base.im.domain.vo.ImMessageVo;
import com.sqx.app.base.im.enums.ImBizType;
import com.sqx.app.base.im.enums.ImMsgType;
import com.sqx.app.base.im.enums.ImSenderRole;
import com.sqx.app.base.im.mapper.ImConversationMapper;
import com.sqx.app.base.im.mapper.ImMessageMapper;
import com.sqx.app.base.im.service.ImService;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.support.UserAvatarSupport;
import com.sqx.app.base.notification.core.enums.NotificationChannel;
import com.sqx.app.base.notification.core.model.NotificationRequest;
import com.sqx.app.base.notification.core.service.INotificationService;
import com.sqx.app.merchant.apply.domain.entity.Merchant;
import com.sqx.app.merchant.apply.mapper.MerchantMapper;
import com.sqx.app.merchant.support.MerchantContextSupport;
import com.sqx.app.rental.goods.domain.entity.RentGoods;
import com.sqx.app.rental.goods.mapper.RentGoodsMapper;
import com.sqx.app.rental.order.domain.entity.RentOrder;
import com.sqx.app.rental.order.domain.entity.RentOrderItem;
import com.sqx.app.rental.order.mapper.RentOrderItemMapper;
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
public class ImServiceImpl implements ImService {

    private static final int PREVIEW_MAX_LEN = 80;
    private static final int TYPE_IM = 2;

    @Resource
    private ImConversationMapper imConversationMapper;
    @Resource
    private ImMessageMapper imMessageMapper;
    @Resource
    private MerchantMapper merchantMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MerchantContextSupport merchantContextSupport;
    @Resource
    private RentOrderItemMapper rentOrderItemMapper;
    @Resource
    private RentGoodsMapper rentGoodsMapper;
    @Resource
    private INotificationService notificationService;

    @Override
    public IPage<ImConversationVo> userConversationPage(Long userId, ImConversationPageBo pageBo) {
        Page<ImConversation> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getUserId, userId)
                .orderByDesc(ImConversation::getLastMsgTime)
                .orderByDesc(ImConversation::getId);
        IPage<ImConversation> convPage = imConversationMapper.selectPage(page, wrapper);
        return PageUtil.copyPage(convPage, toUserConversationVoList(convPage.getRecords()));
    }

    @Override
    public IPage<ImConversationVo> merchantConversationPage(Long userId, ImConversationPageBo pageBo) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        Page<ImConversation> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<ImConversation> wrapper = new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getMerchantId, ctx.getMerchantId())
                .orderByDesc(ImConversation::getLastMsgTime)
                .orderByDesc(ImConversation::getId);
        IPage<ImConversation> convPage = imConversationMapper.selectPage(page, wrapper);
        return PageUtil.copyPage(convPage, toMerchantConversationVoList(convPage.getRecords()));
    }

    @Override
    public IPage<ImMessageVo> userMessagePage(Long userId, ImMessagePageBo pageBo) {
        ImConversation conversation = requireUserConversation(userId, pageBo.getConversationId());
        return messagePage(conversation, pageBo);
    }

    @Override
    public IPage<ImMessageVo> merchantMessagePage(Long userId, ImMessagePageBo pageBo) {
        ImConversation conversation = requireMerchantConversation(userId, pageBo.getConversationId());
        return messagePage(conversation, pageBo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userSendText(Long userId, ImSendTextBo sendBo) {
        ImConversation conversation = requireUserConversation(userId, sendBo.getConversationId());
        insertMessage(conversation, ImSenderRole.USER.getCode(), userId, ImMsgType.TEXT.getCode(),
                sendBo.getContent().trim(), null, null, null, null);
        bumpConversation(conversation.getId(), sendBo.getContent().trim(), false);
        notifyImRecipient(conversation, false, sendBo.getContent().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantSendText(Long userId, ImSendTextBo sendBo) {
        ImConversation conversation = requireMerchantConversation(userId, sendBo.getConversationId());
        insertMessage(conversation, ImSenderRole.MERCHANT.getCode(), userId, ImMsgType.TEXT.getCode(),
                sendBo.getContent().trim(), null, null, null, null);
        bumpConversation(conversation.getId(), sendBo.getContent().trim(), true);
        notifyImRecipient(conversation, true, sendBo.getContent().trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userMarkRead(Long userId, Long conversationId) {
        ImConversation conversation = requireUserConversation(userId, conversationId);
        imConversationMapper.update(null, new LambdaUpdateWrapper<ImConversation>()
                .eq(ImConversation::getId, conversation.getId())
                .set(ImConversation::getUserUnread, 0));
        markMessagesRead(conversation.getId(), ImSenderRole.MERCHANT.getCode(), ImSenderRole.SYSTEM.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merchantMarkRead(Long userId, Long conversationId) {
        ImConversation conversation = requireMerchantConversation(userId, conversationId);
        imConversationMapper.update(null, new LambdaUpdateWrapper<ImConversation>()
                .eq(ImConversation::getId, conversation.getId())
                .set(ImConversation::getMerchantUnread, 0));
        markMessagesRead(conversation.getId(), ImSenderRole.USER.getCode());
    }

    @Override
    public ImConversationVo userOpenByMerchant(Long userId, Long merchantId) {
        if (merchantId == null) {
            throw new BizException("商户ID不能为空");
        }
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw new BizException("商户不存在");
        }
        ImConversation conversation = getOrCreateConversation(userId, merchantId);
        return toUserConversationVo(conversation, merchant, null);
    }

    @Override
    public ImConversationVo merchantOpenByUser(Long userId, Long targetUserId) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        if (targetUserId == null) {
            throw new BizException("用户ID不能为空");
        }
        ImConversation conversation = getOrCreateConversation(targetUserId, ctx.getMerchantId());
        UserEntity user = userMapper.selectById(targetUserId);
        return toMerchantConversationVo(conversation, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendTutorialVideoAfterPickup(RentOrder order) {
        if (order == null || order.getUserId() == null || order.getMerchantId() == null) {
            return;
        }
        List<RentOrderItem> items = rentOrderItemMapper.selectList(new LambdaQueryWrapper<RentOrderItem>()
                .eq(RentOrderItem::getOrderId, order.getId()));
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        ImConversation conversation = getOrCreateConversation(order.getUserId(), order.getMerchantId());
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        Long senderUserId = merchant == null ? null : merchant.getUserId();

        for (RentOrderItem item : items) {
            if (item.getGoodsId() == null) {
                continue;
            }
            RentGoods goods = rentGoodsMapper.selectById(item.getGoodsId());
            if (goods == null || StringUtils.isBlank(goods.getTutorialVideoUrl())) {
                continue;
            }
            Long exists = imMessageMapper.selectCount(new LambdaQueryWrapper<ImMessage>()
                    .eq(ImMessage::getOrderId, order.getId())
                    .eq(ImMessage::getGoodsId, item.getGoodsId())
                    .eq(ImMessage::getBizType, ImBizType.TUTORIAL_VIDEO.getCode()));
            if (exists != null && exists > 0) {
                continue;
            }
            String content = String.format("【%s】使用教程视频，请查收。", StringUtils.defaultIfBlank(item.getGoodsName(), goods.getName()));
            insertMessage(conversation, ImSenderRole.MERCHANT.getCode(), senderUserId,
                    ImMsgType.VIDEO.getCode(), content, goods.getTutorialVideoUrl(),
                    order.getId(), item.getGoodsId(), ImBizType.TUTORIAL_VIDEO.getCode());
            bumpConversation(conversation.getId(), "[使用教程视频] " + content, true);
            log.info("已发送教程视频 IM orderId={} goodsId={}", order.getId(), item.getGoodsId());
        }
    }

    private IPage<ImMessageVo> messagePage(ImConversation conversation, ImMessagePageBo pageBo) {
        Page<ImMessage> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<ImMessage> wrapper = new LambdaQueryWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversation.getId())
                .orderByDesc(ImMessage::getCreateTime)
                .orderByDesc(ImMessage::getId);
        IPage<ImMessage> msgPage = imMessageMapper.selectPage(page, wrapper);
        List<ImMessageVo> voList = msgPage.getRecords().stream()
                .map(this::toMessageVo)
                .collect(Collectors.toList());
        Collections.reverse(voList);
        return PageUtil.copyPage(msgPage, voList);
    }

    private ImConversation getOrCreateConversation(Long userId, Long merchantId) {
        ImConversation existing = imConversationMapper.selectOne(new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getUserId, userId)
                .eq(ImConversation::getMerchantId, merchantId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ImConversation conversation = new ImConversation();
        conversation.setUserId(userId);
        conversation.setMerchantId(merchantId);
        conversation.setUserUnread(0);
        conversation.setMerchantUnread(0);
        imConversationMapper.insert(conversation);
        return conversation;
    }

    private void insertMessage(ImConversation conversation, String senderRole, Long senderUserId,
                               String msgType, String content, String mediaUrl,
                               Long orderId, Long goodsId, String bizType) {
        ImMessage message = new ImMessage();
        message.setConversationId(conversation.getId());
        message.setSenderRole(senderRole);
        message.setSenderUserId(senderUserId);
        message.setMsgType(msgType);
        message.setContent(content);
        message.setMediaUrl(mediaUrl);
        message.setOrderId(orderId);
        message.setGoodsId(goodsId);
        message.setBizType(bizType);
        message.setIsRead(0);
        imMessageMapper.insert(message);
    }

    private void bumpConversation(Long conversationId, String preview, boolean merchantSent) {
        String text = StringUtils.defaultString(preview, "");
        if (text.length() > PREVIEW_MAX_LEN) {
            text = text.substring(0, PREVIEW_MAX_LEN) + "...";
        }
        LambdaUpdateWrapper<ImConversation> wrapper = new LambdaUpdateWrapper<ImConversation>()
                .eq(ImConversation::getId, conversationId)
                .set(ImConversation::getLastMsgTime, LocalDateTime.now())
                .set(ImConversation::getLastMsgPreview, text);
        if (merchantSent) {
            wrapper.setSql("user_unread = user_unread + 1");
        } else {
            wrapper.setSql("merchant_unread = merchant_unread + 1");
        }
        imConversationMapper.update(null, wrapper);
    }

    private void markMessagesRead(Long conversationId, String... senderRoles) {
        if (senderRoles == null || senderRoles.length == 0) {
            return;
        }
        imMessageMapper.update(null, new LambdaUpdateWrapper<ImMessage>()
                .eq(ImMessage::getConversationId, conversationId)
                .in(ImMessage::getSenderRole, Arrays.asList(senderRoles))
                .eq(ImMessage::getIsRead, 0)
                .set(ImMessage::getIsRead, 1));
    }

    private ImConversation requireUserConversation(Long userId, Long conversationId) {
        ImConversation conversation = imConversationMapper.selectById(conversationId);
        if (conversation == null || !userId.equals(conversation.getUserId())) {
            throw new BizException("会话不存在或无权访问");
        }
        return conversation;
    }

    private ImConversation requireMerchantConversation(Long userId, Long conversationId) {
        MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
        ImConversation conversation = imConversationMapper.selectById(conversationId);
        if (conversation == null || !ctx.getMerchantId().equals(conversation.getMerchantId())) {
            throw new BizException("会话不存在或无权访问");
        }
        return conversation;
    }

    private List<ImConversationVo> toUserConversationVoList(List<ImConversation> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Map<Long, Merchant> merchantMap = loadMerchantMap(records.stream()
                .map(ImConversation::getMerchantId).collect(Collectors.toList()));
        List<ImConversationVo> list = new ArrayList<>();
        for (ImConversation item : records) {
            list.add(toUserConversationVo(item, merchantMap.get(item.getMerchantId()), null));
        }
        return list;
    }

    private List<ImConversationVo> toMerchantConversationVoList(List<ImConversation> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }
        Map<Long, UserEntity> userMap = loadUserMap(records.stream()
                .map(ImConversation::getUserId).collect(Collectors.toList()));
        List<ImConversationVo> list = new ArrayList<>();
        for (ImConversation item : records) {
            list.add(toMerchantConversationVo(item, userMap.get(item.getUserId())));
        }
        return list;
    }

    private ImConversationVo toUserConversationVo(ImConversation conversation, Merchant merchant, UserEntity ignored) {
        ImConversationVo vo = new ImConversationVo();
        BeanUtils.copyProperties(conversation, vo);
        vo.setUnreadCount(conversation.getUserUnread() == null ? 0 : conversation.getUserUnread());
        if (merchant != null) {
            vo.setPeerName(merchant.getName());
            if (merchant.getUserId() != null) {
                UserEntity owner = userMapper.selectById(merchant.getUserId());
                if (owner != null) {
                    vo.setPeerAvatar(UserAvatarSupport.resolveAvatar(owner.getAvatar()));
                }
            }
        }
        return vo;
    }

    private ImConversationVo toMerchantConversationVo(ImConversation conversation, UserEntity user) {
        ImConversationVo vo = new ImConversationVo();
        BeanUtils.copyProperties(conversation, vo);
        vo.setUnreadCount(conversation.getMerchantUnread() == null ? 0 : conversation.getMerchantUnread());
        if (user != null) {
            vo.setPeerName(StringUtils.defaultIfBlank(user.getUserName(), user.getPhone()));
            vo.setPeerAvatar(UserAvatarSupport.resolveAvatar(user.getAvatar()));
        }
        return vo;
    }

    private ImMessageVo toMessageVo(ImMessage message) {
        ImMessageVo vo = new ImMessageVo();
        BeanUtils.copyProperties(message, vo);
        vo.setIsRead(message.getIsRead() != null && message.getIsRead() == 1);
        return vo;
    }

    private Map<Long, Merchant> loadMerchantMap(List<Long> merchantIds) {
        if (CollectionUtils.isEmpty(merchantIds)) {
            return Collections.emptyMap();
        }
        return merchantMapper.selectBatchIds(merchantIds.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Merchant::getId, item -> item, (a, b) -> a));
    }

    private Map<Long, UserEntity> loadUserMap(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds.stream().distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(UserEntity::getUserId, item -> item, (a, b) -> a));
    }

    @Override
    public int countUserUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        List<ImConversation> conversations = imConversationMapper.selectList(new LambdaQueryWrapper<ImConversation>()
                .eq(ImConversation::getUserId, userId)
                .select(ImConversation::getUserUnread));
        return sumUnread(conversations, true);
    }

    @Override
    public int countMerchantUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        try {
            MerchantContextSupport.MerchantContext ctx = merchantContextSupport.requireMerchantContext(userId);
            List<ImConversation> conversations = imConversationMapper.selectList(new LambdaQueryWrapper<ImConversation>()
                    .eq(ImConversation::getMerchantId, ctx.getMerchantId())
                    .select(ImConversation::getMerchantUnread));
            return sumUnread(conversations, false);
        } catch (BizException ex) {
            return 0;
        }
    }

    private int sumUnread(List<ImConversation> conversations, boolean userSide) {
        if (CollectionUtils.isEmpty(conversations)) {
            return 0;
        }
        int total = 0;
        for (ImConversation conversation : conversations) {
            Integer unread = userSide ? conversation.getUserUnread() : conversation.getMerchantUnread();
            if (unread != null && unread > 0) {
                total += unread;
            }
        }
        return total;
    }

    private void notifyImRecipient(ImConversation conversation, boolean merchantSent, String preview) {
        if (conversation == null) {
            return;
        }
        String text = StringUtils.defaultString(preview, "");
        if (text.length() > PREVIEW_MAX_LEN) {
            text = text.substring(0, PREVIEW_MAX_LEN) + "...";
        }
        try {
            if (merchantSent) {
                Merchant merchant = merchantMapper.selectById(conversation.getMerchantId());
                String peerName = merchant == null ? "商家" : merchant.getName();
                sendImNotification(conversation.getUserId(), "商家回复了您的消息",
                        String.format("%s：%s", peerName, text),
                        "/packageContent/im-chat?merchantId=" + conversation.getMerchantId()
                                + "&merchantName=" + encodeUrl(peerName));
            } else {
                Merchant merchant = merchantMapper.selectById(conversation.getMerchantId());
                if (merchant == null || merchant.getUserId() == null) {
                    return;
                }
                UserEntity user = userMapper.selectById(conversation.getUserId());
                String peerName = user == null ? "用户" : StringUtils.defaultIfBlank(user.getUserName(), user.getPhone());
                sendImNotification(merchant.getUserId(), "收到新的客服消息",
                        String.format("%s：%s", peerName, text),
                        "/packageMerchant/im-chat?userId=" + conversation.getUserId()
                                + "&userName=" + encodeUrl(peerName));
            }
        } catch (Exception e) {
            log.warn("IM 通知发送失败 conversationId={}: {}", conversation.getId(), e.getMessage());
        }
    }

    private void sendImNotification(Long userId, String title, String content, String url) {
        if (userId == null) {
            return;
        }
        NotificationRequest request = new NotificationRequest();
        request.setUserId(userId);
        request.setTitle(title);
        request.setContent(content);
        request.setType(TYPE_IM);
        request.setUrl(url);
        notificationService.send(request, NotificationChannel.INSTATION.code());
    }

    private static String encodeUrl(String value) {
        try {
            return java.net.URLEncoder.encode(StringUtils.defaultString(value, ""), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
