package com.sqx.app.base.platformcs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsConversationPageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsMessagePageBo;
import com.sqx.app.base.platformcs.domain.bo.PlatformCsSendTextBo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsConversationVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsMessageVo;
import com.sqx.app.base.platformcs.domain.vo.PlatformCsUnreadSummaryVo;

public interface PlatformCsService {

    PlatformCsConversationVo userOpen(Long userId);

    IPage<PlatformCsMessageVo> userMessagePage(Long userId, PlatformCsMessagePageBo pageBo);

    void userSendText(Long userId, PlatformCsSendTextBo sendBo);

    void userMarkRead(Long userId, Long conversationId);

    int countUserUnread(Long userId);

    IPage<PlatformCsConversationVo> adminConversationPage(PlatformCsConversationPageBo pageBo);

    PlatformCsConversationVo adminOpenByUser(Long userId);

    IPage<PlatformCsMessageVo> adminMessagePage(PlatformCsMessagePageBo pageBo);

    void adminSendText(Long adminUserId, PlatformCsSendTextBo sendBo);

    void adminMarkRead(Long conversationId);

    PlatformCsUnreadSummaryVo adminUnreadSummary();
}
