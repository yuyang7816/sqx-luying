package com.sqx.app.base.auth.service;

import com.sqx.app.base.auth.domain.bo.*;
import com.sqx.app.base.auth.domain.vo.InviteCodeInfoVo;
import com.sqx.app.base.auth.domain.vo.LoginVO;
import com.sqx.app.base.auth.domain.vo.WxLoginVo;
import com.sqx.app.base.utils.wx.WxUserPhone;
import com.sqx.common.core.domain.Result;

/**
 * @author chengming
 * @since 2025/4/22
 */
public interface AppAuthService {
    Result<LoginVO> phonePasswordLogin(PhonePasswordLoginBO phonePasswordLoginBO);

    Result<LoginVO> phoneCodeLogin(PhoneCodeLoginBO phoneCodeLoginBO);

    Result<LoginVO> phoneRegister(PhoneRegisterBO phoneRegisterBO);

    Result<Void> resetPassword(ResetPasswordBO resetPasswordBO);


    Result<WxLoginVo> wxLogin(String code);

    Result<WxLoginVo> wxMpLogin(String code);

    Result<LoginVO> wxJsRegister(WxJsRegisterBo wxJsRegisterBo);

    Result<LoginVO> wxMpRegister(WxJsRegisterBo wxJsRegisterBo);

    Result<Object> decryptPhone(DecryptPhoneBo decryptPhoneBo);

    WxUserPhone getWxUserPhoneNumber(String code, String openId);

    Result<InviteCodeInfoVo> inviteCodeInfo(String inviteCode);

    Result<String> inviteWxQrCode(String inviteCode);
}
