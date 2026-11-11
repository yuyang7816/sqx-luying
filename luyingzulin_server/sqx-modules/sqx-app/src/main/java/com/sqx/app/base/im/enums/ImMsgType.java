package com.sqx.app.base.im.enums;

import lombok.Getter;

@Getter
public enum ImMsgType {
    TEXT("text"),
    VIDEO("video"),
    IMAGE("image");

    private final String code;

    ImMsgType(String code) {
        this.code = code;
    }
}
