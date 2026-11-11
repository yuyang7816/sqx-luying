package com.sqx.common.core.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 管理端 keyword 解析：纯数字按主键 ID 精确匹配，否则走名称等模糊搜索。
 */
public final class SearchKeywordUtil {

    private SearchKeywordUtil() {
    }

    /**
     * keyword 为纯数字时解析为 Long ID，否则返回 null。
     */
    public static Long parseLongId(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        String trimmed = keyword.trim();
        if (!trimmed.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
