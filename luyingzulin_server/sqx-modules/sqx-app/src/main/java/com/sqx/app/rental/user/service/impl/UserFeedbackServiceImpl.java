package com.sqx.app.rental.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sqx.app.base.user.db.mapper.UserMapper;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.rental.user.domain.bo.UserFeedbackPageBo;
import com.sqx.app.rental.user.domain.bo.UserFeedbackProcessBo;
import com.sqx.app.rental.user.domain.bo.UserFeedbackSubmitBo;
import com.sqx.app.rental.user.domain.entity.UserFeedback;
import com.sqx.app.rental.user.domain.vo.UserFeedbackVo;
import com.sqx.app.rental.user.enums.FeedbackStatus;
import com.sqx.app.rental.user.mapper.UserFeedbackMapper;
import com.sqx.app.rental.user.service.UserFeedbackService;
import com.sqx.app.rental.user.support.RentalUserSupport;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.core.utils.SearchKeywordUtil;
import com.sqx.common.mybatis.utils.PageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserFeedbackServiceImpl implements UserFeedbackService {

    @Resource
    private UserFeedbackMapper userFeedbackMapper;
    @Resource
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, UserFeedbackSubmitBo submitBo) {
        UserFeedback feedback = new UserFeedback();
        feedback.setFeedbackNo("FB" + GenUtil.genOrderNo());
        feedback.setUserId(userId);
        feedback.setFeedbackType(submitBo.getFeedbackType());
        feedback.setContent(submitBo.getContent());
        feedback.setContactPhone(submitBo.getContactPhone());
        feedback.setImageUrls(joinImages(submitBo.getImageUrls()));
        feedback.setStatus(FeedbackStatus.PENDING.getCode());
        userFeedbackMapper.insert(feedback);
    }

    @Override
    public IPage<UserFeedbackVo> adminPage(UserFeedbackPageBo pageBo) {
        Page<UserFeedback> page = PageUtil.initPage(pageBo.getPage(), pageBo.getLimit());
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(pageBo.getFeedbackType()), UserFeedback::getFeedbackType, pageBo.getFeedbackType())
                .eq(StringUtils.isNotBlank(pageBo.getStatus()), UserFeedback::getStatus, pageBo.getStatus())
                .orderByDesc(UserFeedback::getCreateTime);
        if (StringUtils.isNotBlank(pageBo.getKeyword())) {
            Long id = SearchKeywordUtil.parseLongId(pageBo.getKeyword());
            if (id != null) {
                wrapper.eq(UserFeedback::getId, id);
            } else {
                wrapper.like(UserFeedback::getContent, pageBo.getKeyword());
            }
        }

        if (StringUtils.isNotBlank(pageBo.getPhone())) {
            List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                    .like(UserEntity::getPhone, pageBo.getPhone()));
            if (CollectionUtils.isEmpty(users)) {
                return PageUtil.copyPage(page, Collections.emptyList());
            }
            wrapper.in(UserFeedback::getUserId, users.stream().map(UserEntity::getUserId).collect(Collectors.toList()));
        }

        IPage<UserFeedback> feedbackPage = userFeedbackMapper.selectPage(page, wrapper);
        if (CollectionUtils.isEmpty(feedbackPage.getRecords())) {
            return PageUtil.copyPage(feedbackPage, Collections.emptyList());
        }
        List<Long> userIds = feedbackPage.getRecords().stream().map(UserFeedback::getUserId).distinct()
                .collect(Collectors.toList());
        Map<Long, UserEntity> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, u -> u, (a, b) -> a));
        List<UserFeedbackVo> records = feedbackPage.getRecords().stream()
                .map(f -> toVo(f, userMap.get(f.getUserId())))
                .collect(Collectors.toList());
        return PageUtil.copyPage(feedbackPage, records);
    }

    @Override
    public UserFeedbackVo adminInfo(Long id) {
        UserFeedback feedback = userFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException("反馈不存在");
        }
        UserEntity user = userMapper.selectById(feedback.getUserId());
        return toVo(feedback, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(UserFeedbackProcessBo processBo) {
        int updated = userFeedbackMapper.update(null, new LambdaUpdateWrapper<UserFeedback>()
                .eq(UserFeedback::getId, processBo.getId())
                .eq(UserFeedback::getStatus, FeedbackStatus.PENDING.getCode())
                .set(UserFeedback::getStatus, FeedbackStatus.PROCESSED.getCode())
                .set(UserFeedback::getReplyRemark, processBo.getReplyRemark())
                .set(UserFeedback::getProcessTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BizException("反馈已处理或不存在");
        }
    }

    private UserFeedbackVo toVo(UserFeedback feedback, UserEntity user) {
        UserFeedbackVo vo = new UserFeedbackVo();
        vo.setId(feedback.getId());
        vo.setFeedbackNo(feedback.getFeedbackNo());
        vo.setUserId(feedback.getUserId());
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setFeedbackTypeLabel(resolveFeedbackTypeLabel(feedback.getFeedbackType()));
        vo.setContent(feedback.getContent());
        vo.setContactPhone(feedback.getContactPhone());
        vo.setImageUrls(splitImages(feedback.getImageUrls()));
        vo.setStatus(feedback.getStatus());
        vo.setStatusLabel(FeedbackStatus.PROCESSED.getCode().equals(feedback.getStatus()) ? "已处理" : "待处理");
        vo.setReplyRemark(feedback.getReplyRemark());
        vo.setProcessTime(feedback.getProcessTime());
        vo.setCreateTime(feedback.getCreateTime());
        if (user != null) {
            vo.setPhone(user.getPhone());
            vo.setPhoneMasked(RentalUserSupport.maskPhone(user.getPhone()));
            vo.setUserName(user.getUserName());
        }
        return vo;
    }

    private static String resolveFeedbackTypeLabel(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case "order": return "订单问题";
            case "order_service": return "订单服务";
            case "product": return "商品问题";
            case "suggestion": return "建议反馈";
            default: return "其他";
        }
    }

    private static String joinImages(List<String> imageUrls) {
        if (CollectionUtils.isEmpty(imageUrls)) {
            return null;
        }
        return String.join(",", imageUrls);
    }

    static List<String> splitImages(String imageUrls) {
        if (StringUtils.isBlank(imageUrls)) {
            return Collections.emptyList();
        }
        return Arrays.stream(imageUrls.split(","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
