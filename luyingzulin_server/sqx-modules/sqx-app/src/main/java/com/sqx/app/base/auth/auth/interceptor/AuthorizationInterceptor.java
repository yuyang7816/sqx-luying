package com.sqx.app.base.auth.auth.interceptor;


import com.sqx.app.base.auth.auth.annotation.Login;
import com.sqx.common.core.exception.SqxException;
import com.sqx.common.shiro.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限(Token)验证
 *
 */
@Component
public class AuthorizationInterceptor extends HandlerInterceptorAdapter {
    @Autowired
    private JwtUtil jwtUtil;

    public static final String USER_KEY = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Login annotation;
        if(handler instanceof HandlerMethod) {
            annotation = ((HandlerMethod) handler).getMethodAnnotation(Login.class);
        }else{
            return true;
        }

        if(annotation == null){
            return true;
        }

        //获取用户凭证
        String token = request.getHeader(jwtUtil.getHeader());
        if(StringUtils.isBlank(token)){
            token = request.getParameter(jwtUtil.getHeader());
        }

        //凭证为空
        if(StringUtils.isBlank(token)){
            throw new SqxException(jwtUtil.getHeader() + "不能为空", HttpStatus.UNAUTHORIZED.value());
        }

        Claims claims = jwtUtil.getClaimByToken(token);
        if(claims == null || jwtUtil.isTokenExpired(claims.getExpiration())){
            throw new SqxException(jwtUtil.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
        }

        //设置userId到request里，后续根据userId，获取用户信息
        String subject = claims.getSubject();
        request.setAttribute(USER_KEY, Long.valueOf(subject));

        return true;
    }
}
