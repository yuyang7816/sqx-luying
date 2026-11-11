package com.sqx.app.base.user.support;

import org.apache.commons.lang3.StringUtils;

/**
 * 用户头像默认值
 */
public final class UserAvatarSupport {

    public static final String DEFAULT_AVATAR_URL =
            "https://tu.mianyazuju.com/2026/09/01/eed55c6a-a41e-4a4a-b61a-d29d1ded313a.png";

    private UserAvatarSupport() {
    }

    public static String resolveAvatar(String avatar) {
        return StringUtils.isNotBlank(avatar) ? avatar.trim() : DEFAULT_AVATAR_URL;
    }
}
