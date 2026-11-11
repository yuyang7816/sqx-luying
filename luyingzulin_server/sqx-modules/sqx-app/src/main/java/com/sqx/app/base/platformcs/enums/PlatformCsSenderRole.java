package com.sqx.app.base.platformcs.enums;

import lombok.Getter;

@Getter
public enum PlatformCsSenderRole {
    USER("user"),
    ADMIN("admin");

    private final String code;

    PlatformCsSenderRole(String code) {
        this.code = code;
    }
}
