package com.sqx.common.file.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * @author chengming
 * @since 2025/4/29
 */
public class FileUtil {

    /**
     * 获取文件扩展名
     * @param filename 文件名
     * @return 文件扩展名
     */
    public static String getFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }

        int dotIndex = filename.lastIndexOf(46);
        return -1 == dotIndex ? "" : filename.substring(dotIndex + 1);
    }

    /** OSS / CDN 对象 key 统一使用正斜杠，避免 Windows 下 File.separator 导致访问 404 */
    public static String normalizeObjectKey(String objectKey) {
        if (StringUtils.isBlank(objectKey)) {
            return "";
        }
        return objectKey.replace('\\', '/');
    }

    /** 拼接 CDN 域名与对象 key，避免出现双斜杠 */
    public static String joinPublicUrl(String domain, String objectKey) {
        String key = normalizeObjectKey(objectKey);
        if (StringUtils.isBlank(domain)) {
            return key;
        }
        String base = domain.trim().replaceAll("/+$", "");
        String normalizedKey = key.replaceAll("^/+", "");
        return base + "/" + normalizedKey;
    }
}
