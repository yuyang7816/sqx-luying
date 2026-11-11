package com.sqx.app.base.verifyCode.service;

import com.sqx.app.base.verifyCode.domain.entity.VerifyCode;

/**
 * 验证码 服务类
 *
 * @author sqx
 * @since 2025-04-22 15:31:50
 */
public interface VerifyCodeService {

    /**
     * 校验验证码
     * @param phone 手机号
     * @param code 验证码
     */
    Boolean verify(String phone, String code);

    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param bizCode 业务类型
     */
    VerifyCode sendPhoneCode(String phone, String bizCode);

    /**
     * 发送邮件验证码
     * @param email 邮箱
     * @param bizCode 业务类型
     */
    VerifyCode sendMailCode(String email, String bizCode);
}
