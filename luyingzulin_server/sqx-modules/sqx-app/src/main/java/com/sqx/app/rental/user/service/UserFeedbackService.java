package com.sqx.app.rental.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sqx.app.rental.user.domain.bo.UserFeedbackPageBo;
import com.sqx.app.rental.user.domain.bo.UserFeedbackProcessBo;
import com.sqx.app.rental.user.domain.bo.UserFeedbackSubmitBo;
import com.sqx.app.rental.user.domain.vo.UserFeedbackVo;

public interface UserFeedbackService {

    void submit(Long userId, UserFeedbackSubmitBo submitBo);

    IPage<UserFeedbackVo> adminPage(UserFeedbackPageBo pageBo);

    UserFeedbackVo adminInfo(Long id);

    void process(UserFeedbackProcessBo processBo);
}
