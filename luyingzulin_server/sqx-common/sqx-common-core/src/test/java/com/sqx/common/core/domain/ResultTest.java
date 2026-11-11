package com.sqx.common.core.domain;

import com.alibaba.fastjson2.JSON;
import com.sqx.common.core.enums.SystemErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * M00：Result 统一响应包装单元测试
 */
class ResultTest {

    @Test
    void successWithoutData() {
        Result<Void> result = Result.success();
        assertEquals(SystemErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(SystemErrorCode.SUCCESS.getMessage(), result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void successWithDataAndCustomMessage() {
        Map<String, String> payload = Collections.singletonMap("name", "露营帐篷");
        Result<Map<String, String>> result = Result.success(payload, "查询成功");
        assertEquals(SystemErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("查询成功", result.getMsg());
        assertEquals(payload, result.getData());
    }

    @Test
    void failedWithErrorCodeAndMessage() {
        Result<Void> result = Result.failed(SystemErrorCode.VALIDATE_FAILED, "手机号格式不正确");
        assertEquals(SystemErrorCode.VALIDATE_FAILED.getCode(), result.getCode());
        assertEquals("手机号格式不正确", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void unauthorizedAndForbidden() {
        Result<String> unauthorized = Result.unauthorized("token-expired");
        assertEquals(SystemErrorCode.UNAUTHORIZED.getCode(), unauthorized.getCode());
        assertEquals("token-expired", unauthorized.getData());

        Result<String> forbidden = Result.forbidden("no-permission");
        assertEquals(SystemErrorCode.FORBIDDEN.getCode(), forbidden.getCode());
        assertEquals("no-permission", forbidden.getData());
    }

    @Test
    void jsonSerializeUsesCodeMsgDataFields() {
        Result<String> result = Result.success("ok");
        String json = JSON.toJSONString(result);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"code\":0"));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"msg\":\"操作成功\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"data\":\"ok\""));
    }
}
