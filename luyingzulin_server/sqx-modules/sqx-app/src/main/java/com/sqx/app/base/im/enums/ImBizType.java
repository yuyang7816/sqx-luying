package com.sqx.app.base.im.enums;

import lombok.Getter;

@Getter
public enum ImBizType {
    TUTORIAL_VIDEO("tutorial_video");

    private final String code;

    ImBizType(String code) {
        this.code = code;
    }
}
