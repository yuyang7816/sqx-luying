package com.sqx.app.base.utils.wx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 公众号 OAuth2 access_token 响应
 */
@Data
public class WxMpOAuthRes {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private String openid;

    private String scope;

    private String unionid;

    private Integer errcode;

    private String errmsg;
}
