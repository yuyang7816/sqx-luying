package com.sqx.app.base.im.enums;

import lombok.Getter;

@Getter
public enum ImSenderRole {
    USER("user"),
    MERCHANT("merchant"),
    SYSTEM("system");

    private final String code;

    ImSenderRole(String code) {
        this.code = code;
    }
}
