package com.sqx.app.base.auth.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sqx.app.base.auth.domain.bo.*;
import com.sqx.app.base.auth.domain.vo.InviteCodeInfoVo;
import com.sqx.app.base.auth.domain.vo.LoginVO;
import com.sqx.app.base.auth.domain.vo.WxLoginVo;
import com.sqx.app.base.auth.service.AppAuthService;
import com.sqx.app.base.constants.SysConfigConstant;
import com.sqx.app.base.enums.AppError;
import com.sqx.app.base.enums.ClientEnum;
import com.sqx.app.base.user.db.service.IUserService;
import com.sqx.app.base.user.domain.entity.UserEntity;
import com.sqx.app.base.user.enums.UserStatus;
import com.sqx.app.base.user.enums.UserType;
import com.sqx.app.base.user.support.UserAvatarSupport;
import com.sqx.app.base.utils.OaOAuthConfigUtil;
import com.sqx.app.base.utils.RequestUtil;
import com.sqx.app.base.utils.SysConfigUtil;
import com.sqx.app.base.utils.wx.WxJsLoginRes;
import com.sqx.app.base.utils.wx.WxMaConfigSupport;
import com.sqx.app.base.utils.wx.WxMpOAuthRes;
import com.sqx.app.base.utils.wx.WxUserPhone;
import com.sqx.app.base.utils.wx.WxUtil;
import com.sqx.app.base.verifyCode.service.VerifyCodeService;
import com.sqx.app.referral.user.db.service.IReferralUserCommissionService;
import com.sqx.app.referral.user.domain.entity.ReferralUserCommission;
import com.sqx.app.wallet.wallet.db.service.IUserWalletService;
import com.sqx.app.wallet.wallet.domain.entity.UserWallet;
import com.sqx.common.core.domain.Result;
import com.sqx.common.core.exception.BizException;
import com.sqx.common.core.utils.GenUtil;
import com.sqx.common.core.utils.Md5Utils;
import com.sqx.common.shiro.utils.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author chengming
 * @since 2025/4/22
 */
@Service
public class AppAuthServiceImpl implements AppAuthService {

    @Resource
    private IUserService iUserService;
    @Resource
    private JwtUtil jwtUtil;
    @Resource
    private VerifyCodeService verifyCodeService;
    @Resource
    private WxUtil wxUtil;
    @Resource
    private IUserWalletService iUserWalletService;
    @Resource
    private IReferralUserCommissionService iReferralUserCommissionService;

    @Override
    public Result<LoginVO> phonePasswordLogin(PhonePasswordLoginBO phonePasswordLoginBO) {
        UserEntity userEntity = getUserByPhone(phonePasswordLoginBO.getPhoneNo());
        if (Objects.isNull(userEntity)) {
            throw new BizException(AppError.PHONE_NOT_REGISTER);
        }

        if (1 != userEntity.getStatus()) {
            throw new BizException(AppError.ACCOUNT_ABNORMAL);
        }

        String encryptPassword = Md5Utils.md5s(phonePasswordLoginBO.getPassword());

        if (!Objects.equals(encryptPassword, userEntity.getPassword())) {
            throw new BizException(AppError.PHONE_OR_PASSWORD_ERROR);
        }

        LoginVO vo = buildLoginResult(userEntity);
        return Result.success(vo);
    }

    @Override
    public Result<LoginVO> phoneCodeLogin(PhoneCodeLoginBO phoneCodeLoginBO) {
        // 校验验证码
        if (!verifyCodeService.verify(phoneCodeLoginBO.getPhoneNo(), phoneCodeLoginBO.getVerifyCode())) {
            throw new BizException(AppError.VERIFY_CODE_ERROR);
        }

        UserEntity userEntity = getUserByPhone(phoneCodeLoginBO.getPhoneNo());
        if (Objects.isNull(userEntity)) {
            // 注册流程
            UserEntity userInfo = registerPhone(phoneCodeLoginBO.getPhoneNo(), null ,null,null, null, null);
            LoginVO vo = buildLoginResult(userInfo);
            return Result.success(vo);
        }

        if (1 != userEntity.getStatus()) {
            throw new BizException(AppError.ACCOUNT_ABNORMAL);
        }

        LoginVO vo = buildLoginResult(userEntity);
        return Result.success(vo);
    }

    @Override
    public Result<LoginVO> phoneRegister(PhoneRegisterBO phoneRegisterBO) {
        if (!verifyCodeService.verify(phoneRegisterBO.getPhoneNo(), phoneRegisterBO.getCode())) {
            throw new BizException(AppError.VERIFY_CODE_ERROR);
        }

        UserEntity userInfo = registerPhone(phoneRegisterBO.getPhoneNo(), phoneRegisterBO.getPassword(), phoneRegisterBO.getUsername(), phoneRegisterBO.getSysPhone(), phoneRegisterBO.getPlatform(), phoneRegisterBO.getSuperiorInvitationCode());

        LoginVO vo = buildLoginResult(userInfo);
        return Result.success(vo);
    }

    @Override
    public Result<Void> resetPassword(ResetPasswordBO resetPasswordBO) {
        if (!verifyCodeService.verify(resetPasswordBO.getPhone(), resetPasswordBO.getCode())) {
            throw new BizException(AppError.VERIFY_CODE_ERROR);
        }

        if (!checkPhoneExist(resetPasswordBO.getPhone())) {
            throw new BizException(AppError.PHONE_NOT_REGISTER);
        }

        UpdateWrapper<UserEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda()
                .eq(UserEntity::getPhone, resetPasswordBO.getPhone())
                .set(UserEntity::getPassword, Md5Utils.md5s(resetPasswordBO.getPassword()));

        iUserService.update(new UserEntity(), updateWrapper);

        return Result.success();
    }

    private boolean checkPhoneExist(String phone) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getPhone, phone);
        return iUserService.exists(queryWrapper);
    }

    /**
     * 手机号注册
     * @param phoneNo
     * @param sysPhone
     * @param platform
     * @param superiorInvitationCode
     * @return
     */
    private UserEntity registerPhone(String phoneNo, String password, String username, Integer sysPhone, String platform, String superiorInvitationCode) {
        UserEntity userEntity = getUserByPhone(phoneNo);
        if (Objects.nonNull(userEntity)) {
            throw new BizException(AppError.PHONE_REGISTERED);
        }

        validateSuperiorInvitationCode(superiorInvitationCode);

        UserEntity userInfo = new UserEntity();
        userInfo.setUserName(phoneNo);
        userInfo.setPhone(phoneNo);
        userInfo.setSysPhone(sysPhone);
        userInfo.setStatus(UserStatus.ENABLE.getCode());
        userInfo.setPlatform(platform);
        userInfo.setUserType(UserType.USER.getCode());
        userInfo.setIsVip(0);
        userInfo.setInvitationCode(GenUtil.genInviteCode(phoneNo));
        userInfo.setSuperiorInvitationCode(normalizeInvitationCode(superiorInvitationCode));

        if (StringUtils.isNotBlank(password)) {
            String encryptPassword = Md5Utils.md5s(password);
            userInfo.setPassword(encryptPassword);
        }

        iUserService.save(userInfo);

        /* 其他业务 */
        registerBiz(userInfo.getUserId());

        return userInfo;
    }

    private void registerBiz(Long userId) {
        UserWallet userWallet = new UserWallet();
        userWallet.setUserId(userId);
        userWallet.setAmount(BigDecimal.ZERO);
        iUserWalletService.save(userWallet);

        ReferralUserCommission referralUserCommission = new ReferralUserCommission();
        referralUserCommission.setUserId(userId);
        referralUserCommission.setCommission(BigDecimal.ZERO);
        iReferralUserCommissionService.save(referralUserCommission);
    }

    @Override
    public Result<WxLoginVo> wxLogin(String code) {
        WxMaConfigSupport.MaCredentials credentials = WxMaConfigSupport.resolveCredentials();
        WxJsLoginRes wxJsLoginRes = wxUtil.wxJsLogin(credentials.getAppId(), credentials.getSecret(), code);

        WxLoginVo vo = new WxLoginVo();
        vo.setSessionKey(wxJsLoginRes.getSessionKey());
        vo.setUnionid(wxJsLoginRes.getUnionid());
        vo.setOpenid(wxJsLoginRes.getOpenid());

        // 是否绑定手机号
        UserEntity userEntity = iUserService.getByOpenId(wxJsLoginRes.getOpenid());
        if (Objects.isNull(userEntity) || StringUtils.isBlank(userEntity.getPhone())) {
            vo.setBindPhone(false);
        } else {
            LoginVO loginVO = buildLoginResult(userEntity);
            vo.setLoginVO(loginVO);
            vo.setUserEntity(userEntity);
            vo.setBindPhone(true);
        }

        return Result.success(vo);
    }

    @Override
    public Result<WxLoginVo> wxMpLogin(String code) {
        if (!OaOAuthConfigUtil.isEnabled()) {
            throw new BizException(AppError.OA_OAUTH_DISABLED);
        }
        String appId = SysConfigUtil.getConfigValue(SysConfigConstant.Sys.SYS_USER_MP_APPID);
        String secret = SysConfigUtil.getConfigValue(SysConfigConstant.Sys.SYS_USER_MP_SECRET);

        WxMpOAuthRes oauthRes = wxUtil.wxMpOAuthAccessToken(appId, secret, code);

        WxLoginVo vo = new WxLoginVo();
        vo.setUnionid(oauthRes.getUnionid());
        vo.setOpenid(oauthRes.getOpenid());

        UserEntity userEntity = iUserService.getByMpOpenId(oauthRes.getOpenid());
        if (Objects.isNull(userEntity) || StringUtils.isBlank(userEntity.getPhone())) {
            vo.setBindPhone(false);
        } else {
            LoginVO loginVO = buildLoginResult(userEntity);
            vo.setLoginVO(loginVO);
            vo.setUserEntity(userEntity);
            vo.setBindPhone(true);
        }

        return Result.success(vo);
    }

    @Override
    public Result<LoginVO> wxJsRegister(WxJsRegisterBo wxJsRegisterBo) {
        wxJsRegisterBo.setPhone(resolveWxRegisterPhone(wxJsRegisterBo));
        UserEntity userEntity = iUserService.getByOpenId(wxJsRegisterBo.getOpenId());
        if (Objects.nonNull(userEntity)) {
            if (StringUtils.isBlank(userEntity.getPhone())) {
                userEntity.setPhone(wxJsRegisterBo.getPhone());
                iUserService.updateById(userEntity);
            }

            // 已注册过, 直接登录
            LoginVO vo = buildLoginResult(userEntity);
            return Result.success(vo);
        }

        UserEntity userByPhone = getUserByPhone(wxJsRegisterBo.getPhone());
        if (Objects.nonNull(userByPhone)) {

            if (StringUtils.isBlank(userByPhone.getMaOpenId())) {
                userByPhone.setMaOpenId(wxJsRegisterBo.getOpenId());
                iUserService.updateById(userByPhone);
            }

            // 已注册过, 直接登录
            LoginVO vo = buildLoginResult(userByPhone);
            return Result.success(vo);
        }

        UserEntity userInfo = new UserEntity();
        userInfo.setUserName(StringUtils.isNotBlank(wxJsRegisterBo.getPhone()) ? wxJsRegisterBo.getPhone() : wxJsRegisterBo.getUsername());
        userInfo.setAvatar(wxJsRegisterBo.getAvatar());
        userInfo.setPhone(wxJsRegisterBo.getPhone());
        userInfo.setStatus(UserStatus.ENABLE.getCode());
        userInfo.setPlatform("JS");
        userInfo.setUserType(UserType.USER.getCode());
        userInfo.setIsVip(0);
        userInfo.setInvitationCode(GenUtil.genInviteCode(wxJsRegisterBo.getPhone()));
        validateSuperiorInvitationCode(wxJsRegisterBo.getSuperiorInvitationCode());
        userInfo.setSuperiorInvitationCode(normalizeInvitationCode(wxJsRegisterBo.getSuperiorInvitationCode()));
        userInfo.setMaOpenId(wxJsRegisterBo.getOpenId());
        userInfo.setCreateTime(LocalDateTime.now());

        iUserService.save(userInfo);

        /* 其他业务 */
        registerBiz(userInfo.getUserId());

        LoginVO vo = buildLoginResult(userInfo);
        return Result.success(vo);
    }

    @Override
    public Result<LoginVO> wxMpRegister(WxJsRegisterBo wxJsRegisterBo) {
        UserEntity userEntity = iUserService.getByMpOpenId(wxJsRegisterBo.getOpenId());
        if (Objects.nonNull(userEntity)) {
            if (StringUtils.isBlank(userEntity.getPhone())) {
                userEntity.setPhone(wxJsRegisterBo.getPhone());
                iUserService.updateById(userEntity);
            }
            return Result.success(buildLoginResult(userEntity));
        }

        UserEntity userByPhone = getUserByPhone(wxJsRegisterBo.getPhone());
        if (Objects.nonNull(userByPhone)) {
            if (StringUtils.isBlank(userByPhone.getMpOpenId())) {
                userByPhone.setMpOpenId(wxJsRegisterBo.getOpenId());
                if (StringUtils.isNotBlank(wxJsRegisterBo.getAvatar())) {
                    userByPhone.setAvatar(wxJsRegisterBo.getAvatar());
                }
                iUserService.updateById(userByPhone);
            }
            return Result.success(buildLoginResult(userByPhone));
        }

        UserEntity userInfo = new UserEntity();
        userInfo.setUserName(wxJsRegisterBo.getPhone());
        userInfo.setAvatar(wxJsRegisterBo.getAvatar());
        userInfo.setPhone(wxJsRegisterBo.getPhone());
        userInfo.setStatus(UserStatus.ENABLE.getCode());
        userInfo.setPlatform("公众号");
        userInfo.setUserType(UserType.USER.getCode());
        userInfo.setIsVip(0);
        userInfo.setInvitationCode(GenUtil.genInviteCode(wxJsRegisterBo.getPhone()));
        validateSuperiorInvitationCode(wxJsRegisterBo.getSuperiorInvitationCode());
        userInfo.setSuperiorInvitationCode(normalizeInvitationCode(wxJsRegisterBo.getSuperiorInvitationCode()));
        userInfo.setMpOpenId(wxJsRegisterBo.getOpenId());
        userInfo.setCreateTime(LocalDateTime.now());

        iUserService.save(userInfo);
        registerBiz(userInfo.getUserId());

        return Result.success(buildLoginResult(userInfo));
    }

    @Override
    public Result<Object> decryptPhone(DecryptPhoneBo decryptPhoneBo) {
        String decryptS5 = wxUtil.decryptS5(decryptPhoneBo.getDecryptData(), decryptPhoneBo.getKey(), decryptPhoneBo.getIv());
        return Result.success(JSONObject.parseObject(decryptS5));
    }

    @Override
    public WxUserPhone getWxUserPhoneNumber(String code, String openId) {
        WxUserPhone wxUserPhone = wxUtil.getUserPhoneNumber(code, openId, RequestUtil.getClientType());

        return wxUserPhone;
    }

    @Override
    public Result<InviteCodeInfoVo> inviteCodeInfo(String inviteCode) {
        UserEntity inviter = getUserByInvitationCode(inviteCode);
        if (Objects.isNull(inviter) || !Objects.equals(UserStatus.ENABLE.getCode(), inviter.getStatus())) {
            throw new BizException(AppError.INVITE_CODE_INVALID);
        }
        InviteCodeInfoVo vo = new InviteCodeInfoVo();
        vo.setInviteCode(normalizeInvitationCode(inviteCode));
        vo.setInviterId(inviter.getUserId());
        vo.setInviterName(StringUtils.isNotBlank(inviter.getUserName()) ? inviter.getUserName() : maskPhone(inviter.getPhone()));
        vo.setInviterMobile(maskPhone(inviter.getPhone()));
        return Result.success(vo);
    }

    @Override
    public Result<String> inviteWxQrCode(String inviteCode) {
        UserEntity inviter = getUserByInvitationCode(inviteCode);
        if (Objects.isNull(inviter) || !Objects.equals(UserStatus.ENABLE.getCode(), inviter.getStatus())) {
            throw new BizException(AppError.INVITE_CODE_INVALID);
        }
        String scene = "ic=" + normalizeInvitationCode(inviteCode);
        String base64 = wxUtil.getWxUnlimitedQrCodeBase64(scene, "packageUser/register");
        return Result.success(base64);
    }

    /**
     * 构建登录返回值
     * @param userEntity 用户信息
     * @return
     */
    private LoginVO buildLoginResult(UserEntity userEntity) {
        LoginVO vo = new LoginVO();

        //生成token
        String token = jwtUtil.generateToken(userEntity.getUserId());
        vo.setToken(token);
        vo.setExpire(jwtUtil.getExpire());
        UserEntity responseUser = new UserEntity();
        BeanUtils.copyProperties(userEntity, responseUser);
        responseUser.setAvatar(UserAvatarSupport.resolveAvatar(userEntity.getAvatar()));
        vo.setUser(responseUser);
        return vo;

    }

    /**
     * 解析微信注册手机号：优先明文手机号，否则通过微信官方 code 换取
     */
    private String resolveWxRegisterPhone(WxJsRegisterBo wxJsRegisterBo) {
        if (StringUtils.isNotBlank(wxJsRegisterBo.getPhone())) {
            return wxJsRegisterBo.getPhone().trim();
        }
        if (StringUtils.isNotBlank(wxJsRegisterBo.getPhoneCode())) {
            if (StringUtils.isBlank(wxJsRegisterBo.getOpenId())) {
                throw new BizException("微信授权信息无效，请重新登录");
            }
            WxUserPhone wxUserPhone = wxUtil.getUserPhoneNumber(
                    wxJsRegisterBo.getPhoneCode(),
                    wxJsRegisterBo.getOpenId(),
                    ClientEnum.USER.getCode());
            if (wxUserPhone == null || StringUtils.isBlank(wxUserPhone.getPurePhoneNumber())) {
                throw new BizException("获取手机号失败，请重试");
            }
            return wxUserPhone.getPurePhoneNumber();
        }
        throw new BizException("请授权手机号");
    }

    /**
     * 手机号获取用户信息
     * @param phoneNo 手机号
     * @return
     */
    private UserEntity getUserByPhone(String phoneNo) {
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getPhone, phoneNo);

        return iUserService.getOne(queryWrapper);
    }

    private UserEntity getUserByInvitationCode(String invitationCode) {
        String normalized = normalizeInvitationCode(invitationCode);
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        QueryWrapper<UserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(UserEntity::getInvitationCode, normalized);
        return iUserService.getOne(queryWrapper, false);
    }

    private String normalizeInvitationCode(String invitationCode) {
        return StringUtils.isBlank(invitationCode) ? null : invitationCode.trim().toUpperCase();
    }

    private void validateSuperiorInvitationCode(String superiorInvitationCode) {
        if (StringUtils.isBlank(superiorInvitationCode)) {
            return;
        }
        UserEntity inviter = getUserByInvitationCode(superiorInvitationCode);
        if (Objects.isNull(inviter) || !Objects.equals(UserStatus.ENABLE.getCode(), inviter.getStatus())) {
            throw new BizException(AppError.INVITE_CODE_INVALID);
        }
    }

    private String maskPhone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
