package com.sqx.app.rental.user.enums;

import com.sqx.common.core.exception.BizException;
import lombok.Getter;

/**
 * 反馈状态
 */
@Getter
public enum FeedbackStatus {
    PENDING("pending", "待处理"),
    PROCESSED("processed", "已处理");

    private final String code;
    private final String label;

    FeedbackStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static FeedbackStatus fromCode(String code) {
        for (FeedbackStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new BizException("反馈状态无效");
    }
}
