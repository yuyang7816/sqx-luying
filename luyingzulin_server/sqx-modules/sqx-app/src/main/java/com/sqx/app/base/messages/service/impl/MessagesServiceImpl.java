package com.sqx.app.base.messages.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.common.redis.utils.ConfigUtil;
import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.messages.domain.vo.MessagesSubscribeTemplateVo;
import com.sqx.app.base.messages.domain.vo.MessagesUnreadSummaryVo;
import com.sqx.app.base.messages.domain.vo.MessagesVo;
import com.sqx.app.base.messages.db.service.IMessagesService;
import com.sqx.app.base.messages.domain.bo.MessagesInfoBo;
import com.sqx.app.base.messages.domain.bo.MessagesPageBo;
import com.sqx.app.base.messages.domain.bo.MessagesSaveBo;
import com.sqx.app.base.messages.domain.bo.MessagesUpdateBo;
import com.sqx.app.base.messages.domain.entity.Messages;
import com.sqx.app.base.messages.service.MessagesService;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 消息信息表 服务实现类
 *
 * @author 西安省钱兄网络科技有限公司
 * @since 2025-08-15 16:28:04
 */
@Service
public class MessagesServiceImpl implements MessagesService {

    @Resource
    private IMessagesService iMessagesService;
    @Resource
    private IUserService iUserService;

    @Override
    public void saveInfo(MessagesSaveBo messagesSaveBo) {
        Messages messages = new Messages();
        BeanUtils.copyProperties(messagesSaveBo, messages);

        iMessagesService.save(messages);
    }

    @Override
    public IPage<MessagesVo> pageInfo(MessagesPageBo messagesPageBo) {
        Page<Messages> initPage = PageUtil.initPage(messagesPageBo.getPage(), messagesPageBo.getLimit());

        IPage<MessagesVo> voIPage = iMessagesService.pageInfo(initPage, messagesPageBo);
        return voIPage;
    }

    @Override
    public MessagesVo info(MessagesInfoBo messagesInfoBo) {
        Messages messages = iMessagesService.getById(messagesInfoBo.getId());
        if (Objects.isNull(messages)) {
            throw new BizException("信息不存在");
        }

        MessagesVo vo = new MessagesVo();
        BeanUtils.copyProperties(messages, vo);
        enrichUser(vo);
        return vo;
    }

    @Override
    public void markRead(Long userId, Long messageId) {
        if (messageId == null) {
            return;
        }
        Messages messages = iMessagesService.getById(messageId);
        if (messages == null) {
            throw new BizException("信息不存在");
        }
        if (messages.getUserId() != null && !userId.equals(messages.getUserId())) {
            throw new BizException("无权操作该消息");
        }
        if (Integer.valueOf(1).equals(messages.getIsRead())) {
            return;
        }
        Messages update = new Messages();
        update.setId(messageId);
        update.setIsRead(1);
        iMessagesService.updateById(update);
    }

    private void enrichUser(MessagesVo vo) {
        if (vo.getUserId() == null) {
            return;
        }
        UserEntity user = iUserService.getById(vo.getUserId());
        if (user != null) {
            vo.setUserName(user.getUserName());
            vo.setPhone(user.getPhone());
        }
    }

    @Override
    public void updateInfo(MessagesUpdateBo messagesUpdateBo) {
        Messages messages = new Messages();
        BeanUtils.copyProperties(messagesUpdateBo, messages);

        iMessagesService.updateById(messages);
    }

    @Override
    public void delete(MessagesInfoBo messagesInfoBo) {
        iMessagesService.removeById(messagesInfoBo.getId());
    }

    @Override
    public int countUnread(Long userId, Integer type) {
        if (userId == null) {
            return 0;
        }
        LambdaQueryWrapper<Messages> wrapper = new LambdaQueryWrapper<Messages>()
                .eq(Messages::getUserId, userId)
                .eq(Messages::getIsRead, 0);
        if (type != null) {
            wrapper.eq(Messages::getType, type);
        }
        return Math.toIntExact(iMessagesService.count(wrapper));
    }

    @Override
    public int countSystemNoticeUnread(Long userId) {
        if (userId == null) {
            return 0;
        }
        LambdaQueryWrapper<Messages> wrapper = new LambdaQueryWrapper<Messages>()
                .eq(Messages::getUserId, userId)
                .eq(Messages::getIsRead, 0)
                .notIn(Messages::getType, 2, 4);
        return Math.toIntExact(iMessagesService.count(wrapper));
    }

    @Override
    public MessagesUnreadSummaryVo unreadSummary(Long userId, int imUnread) {
        int systemUnread = countSystemNoticeUnread(userId);
        MessagesUnreadSummaryVo vo = new MessagesUnreadSummaryVo();
        vo.setSystemUnread(systemUnread);
        vo.setImUnread(Math.max(imUnread, 0));
        vo.setTotalUnread(systemUnread + vo.getImUnread());
        return vo;
    }

    @Override
    public MessagesSubscribeTemplateVo subscribeTemplates() {
        String raw = ConfigUtil.getConfigValue(SysConfigConstant.Sys.SYS_USER_MA_SUBSCRIBE_ORDER_NOTIFY);
        List<String> templateIds = Collections.emptyList();
        if (StringUtils.isNotBlank(raw)) {
            templateIds = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
        }
        MessagesSubscribeTemplateVo vo = new MessagesSubscribeTemplateVo();
        vo.setOrderNotify(templateIds);
        return vo;
    }
}
