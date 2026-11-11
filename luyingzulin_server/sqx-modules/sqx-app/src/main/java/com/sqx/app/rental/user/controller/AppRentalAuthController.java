package com.sqx.app.rental.user.controller;

import com.sqx.app.base.auth.domain.bo.*;
import com.sqx.app.base.auth.domain.vo.LoginVO;
import com.sqx.app.base.auth.domain.vo.WxLoginVo;
import com.sqx.app.base.auth.service.AppAuthService;
import com.sqx.app.base.utils.wx.WxUserPhone;
import com.sqx.common.core.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户端登录注册（M05 租赁路径别名，复用 base/auth 能力）
 */
@Tag(name = "用户端-登录(M05)", description = "复用 /app/base/auth，提供 /app/auth 快捷路径")
@RestController
@RequestMapping("/app/auth")
public class AppRentalAuthController {

    @Resource
    private AppAuthService appAuthService;

    @Operation(summary = "手机号密码登录")
    @PostMapping("phonePasswordLogin")
    public Result<LoginVO> phonePasswordLogin(@RequestBody PhonePasswordLoginBO phonePasswordLoginBO) {
        return appAuthService.phonePasswordLogin(phonePasswordLoginBO);
    }

    @Operation(summary = "手机号验证码登录（未注册自动注册）")
    @PostMapping("phoneCodeLogin")
    public Result<LoginVO> phoneCodeLogin(@RequestBody PhoneCodeLoginBO phoneCodeLoginBO) {
        return appAuthService.phoneCodeLogin(phoneCodeLoginBO);
    }

    @Operation(summary = "手机号注册")
    @PostMapping("phoneRegister")
    public Result<LoginVO> phoneRegister(@RequestBody PhoneRegisterBO phoneRegisterBO) {
        return appAuthService.phoneRegister(phoneRegisterBO);
    }

    @Operation(summary = "忘记密码")
    @PostMapping("resetPassword")
    public Result<Void> resetPassword(@RequestBody ResetPasswordBO resetPasswordBO) {
        return appAuthService.resetPassword(resetPasswordBO);
    }

    @Operation(summary = "微信小程序登录")
    @GetMapping("wxLogin")
    public Result<WxLoginVo> wxLogin(@RequestParam("code") String code) {
        return appAuthService.wxLogin(code);
    }

    @Operation(summary = "微信公众号 OAuth 登录")
    @GetMapping("wxMpLogin")
    public Result<WxLoginVo> wxMpLogin(@RequestParam("code") String code) {
        return appAuthService.wxMpLogin(code);
    }

    @Operation(summary = "微信小程序注册")
    @PostMapping("wxJsRegister")
    public Result<LoginVO> wxJsRegister(@RequestBody WxJsRegisterBo wxJsRegisterBo) {
        return appAuthService.wxJsRegister(wxJsRegisterBo);
    }

    @Operation(summary = "微信公众号注册绑定手机号")
    @PostMapping("wxMpRegister")
    public Result<LoginVO> wxMpRegister(@RequestBody WxJsRegisterBo wxJsRegisterBo) {
        return appAuthService.wxMpRegister(wxJsRegisterBo);
    }
}
