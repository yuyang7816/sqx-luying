package com.sqx.app.base.im.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.im.domain.bo.ImConversationPageBo;
import com.sqx.app.base.im.domain.bo.ImMessagePageBo;
import com.sqx.app.base.im.domain.bo.ImSendTextBo;
import com.sqx.app.base.im.domain.vo.ImConversationVo;
import com.sqx.app.base.im.domain.vo.ImMessageVo;
import com.sqx.app.rental.order.domain.entity.RentOrder;

public interface ImService {

    IPage<ImConversationVo> userConversationPage(Long userId, ImConversationPageBo pageBo);

    IPage<ImConversationVo> merchantConversationPage(Long userId, ImConversationPageBo pageBo);

    IPage<ImMessageVo> userMessagePage(Long userId, ImMessagePageBo pageBo);

    IPage<ImMessageVo> merchantMessagePage(Long userId, ImMessagePageBo pageBo);

    void userSendText(Long userId, ImSendTextBo sendBo);

    void merchantSendText(Long userId, ImSendTextBo sendBo);

    void userMarkRead(Long userId, Long conversationId);

    void merchantMarkRead(Long userId, Long conversationId);

    ImConversationVo userOpenByMerchant(Long userId, Long merchantId);

    ImConversationVo merchantOpenByUser(Long userId, Long targetUserId);

    void sendTutorialVideoAfterPickup(RentOrder order);

    int countUserUnread(Long userId);

    int countMerchantUnread(Long userId);
}
