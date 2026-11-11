package com.sqx.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M05 三端联调冒烟：登录守卫(401)、token 一致性、资料/地址/收藏、admin 用户统计。
 * <p>
 * 依赖：server:9135 已启动；MySQL 已执行 m05_user_account.sql 与 m05_user_account_seed.sql。
 */
public class M05IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String TEST_PHONE = "13812348888";
    static final String TEST_PASSWORD = "123456";
    static final long GOODS_ID = 1L;
    static final String RENT_START = "2026-09-15 10:00:00";
    static final String RENT_END = "2026-09-17 18:00:00";

    public static void main(String[] args) throws Exception {
        assertHealthOk();
        assertLoginGuards();

        String authLoginJson = httpPost(BASE + "/app/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}", null);
        assertCodeZero(authLoginJson, "app auth phonePasswordLogin");
        String token = extractNested(authLoginJson, "data", "token");
        assertFalse("登录 token 不应为空", token.isEmpty());

        String legacyLoginJson = httpPost(BASE + "/app/base/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}", null);
        assertCodeZero(legacyLoginJson, "app base auth phonePasswordLogin");
        String legacyToken = extractNested(legacyLoginJson, "data", "token");
        assertFalse("base auth 登录 token 不应为空", legacyToken.isEmpty());

        String infoJson = httpGet(BASE + "/app/user/info", token);
        assertCodeZero(infoJson, "app user info");
        assertContains(infoJson, "phoneMasked", "用户信息应含脱敏手机号");
        assertNotContains(infoJson, "\"phone\":\"" + TEST_PHONE + "\"", "C 端不应返回明文手机号");
        assertNotContains(infoJson, "\"addressCount\"", "C 端资料不应返回 addressCount");

        String profileJson = httpPut(BASE + "/app/user/profile",
                "{\"userName\":\"camp001-smoke\"}", token);
        assertCodeZero(profileJson, "app user profile update");
        assertContains(profileJson, "camp001-smoke", "资料更新应回写昵称");

        String addressListJson = httpGet(BASE + "/app/user/address/list", token);
        assertCodeZero(addressListJson, "app address list");
        assertContains(addressListJson, "contactName", "地址列表应含收货人");

        String favoriteListJson = httpGet(BASE + "/app/user/favorite/list", token);
        assertCodeZero(favoriteListJson, "app favorite list");
        assertContains(favoriteListJson, "goodsName", "收藏列表应含商品名称");

        String checkJson = httpGet(BASE + "/app/user/favorite/check?goodsId=1", token);
        assertCodeZero(checkJson, "app favorite check");

        String toggleJson = httpPost(BASE + "/app/user/favorite/toggle", "{\"goodsId\":1}", token);
        assertCodeZero(toggleJson, "app favorite toggle");

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        String adminToken = adminLogin(uuid, captcha);

        String adminListJson = httpPost(BASE + "/admin/user/list",
                "{\"page\":1,\"limit\":10,\"phone\":\"138\"}", adminToken);
        assertCodeZero(adminListJson, "admin user list");
        assertContains(adminListJson, "phoneMasked", "admin 用户列表应返回脱敏手机号");
        assertContains(adminListJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 用户列表应返回明文手机号");

        String userId = extractNested(adminListJson, "records", "userId");
        if (userId.isEmpty()) {
            userId = extractFirstRecordField(adminListJson, "userId");
        }
        assertFalse("admin 用户列表应含 userId", userId.isEmpty());

        String adminInfoJson = httpGet(BASE + "/admin/user/info?userId=" + userId, adminToken);
        assertCodeZero(adminInfoJson, "admin user info");
        assertContains(adminInfoJson, "phoneMasked", "admin 用户详情应含脱敏手机号");
        assertContains(adminInfoJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 用户详情应返回明文手机号");
        assertContains(adminInfoJson, "addressCount", "admin 用户详情应含地址数量");
        assertContains(adminInfoJson, "favoriteCount", "admin 用户详情应含收藏数量");

        assertAdminOperationalPagesPlainPhone(adminToken, userId);
        assertDepositAdjust(adminToken, userId);

        System.out.println("m05-integration-ok");
    }

    /** 运营页 admin 接口应返回明文 phone（有记录时断言；无种子数据时仅校验接口可用） */
    static void assertAdminOperationalPagesPlainPhone(String adminToken, String userId) throws Exception {
        String depositJson = httpPost(BASE + "/admin/user/deposit/page",
                "{\"page\":1,\"limit\":10,\"phone\":\"" + TEST_PHONE + "\"}", adminToken);
        assertCodeZero(depositJson, "admin deposit page");
        assertPlainPhoneWhenRecordsPresent(depositJson, "admin 押金列表");

        String feedbackJson = httpPost(BASE + "/admin/user/feedback/page",
                "{\"page\":1,\"limit\":10,\"phone\":\"" + TEST_PHONE + "\"}", adminToken);
        assertCodeZero(feedbackJson, "admin feedback page");
        assertPlainPhoneWhenRecordsPresent(feedbackJson, "admin 反馈列表");

        String walletTxJson = httpPost(BASE + "/admin/userWalletTransaction/page",
                "{\"page\":1,\"limit\":10,\"userId\":" + userId + "}", adminToken);
        assertCodeZero(walletTxJson, "admin wallet transaction page");
        assertPlainPhoneWhenRecordsPresent(walletTxJson, "admin 钱包流水");

        String withdrawJson = httpPost(BASE + "/admin/withdrawRecord/page",
                "{\"page\":1,\"limit\":10,\"userId\":" + userId + "}", adminToken);
        assertCodeZero(withdrawJson, "admin withdraw record page");
        assertPlainPhoneWhenRecordsPresent(withdrawJson, "admin 提现记录");

        String withdrawalAccountJson = httpPost(BASE + "/admin/withdrawalAccount/page",
                "{\"page\":1,\"limit\":10,\"userId\":" + userId + ",\"source\":1}", adminToken);
        assertCodeZero(withdrawalAccountJson, "admin withdrawal account page");
        assertPlainPhoneWhenRecordsPresent(withdrawalAccountJson, "admin 提现账户");

        String messagesJson = httpPost(BASE + "/admin/messages/page",
                "{\"page\":1,\"limit\":10,\"phone\":\"" + TEST_PHONE + "\"}", adminToken);
        assertCodeZero(messagesJson, "admin messages page");
        assertPlainPhoneWhenRecordsPresent(messagesJson, "admin 消息列表");
    }

    static void assertPlainPhoneWhenRecordsPresent(String json, String label) {
        if (json == null || !json.contains("\"records\":[")) {
            return;
        }
        int recordsIdx = json.indexOf("\"records\":[");
        if (recordsIdx < 0) {
            return;
        }
        String recordsPart = json.substring(recordsIdx);
        if (recordsPart.startsWith("\"records\":[]")) {
            return;
        }
        assertContains(json, "\"phone\":\"" + TEST_PHONE + "\"", label + " 应返回明文手机号");
    }

    /** 押金人工调增/调减/超限拒绝 */
    static void assertDepositAdjust(String adminToken, String userId) throws Exception {
        String summaryJson = httpPost(BASE + "/admin/user/deposit/page",
                "{\"page\":1,\"limit\":1,\"userId\":" + userId + "}", adminToken);
        assertCodeZero(summaryJson, "admin deposit page before adjust");

        String increaseJson = httpPost(BASE + "/admin/user/deposit/adjust",
                "{\"userId\":" + userId + ",\"direction\":1,\"amount\":10.00,\"remark\":\"M05调增\"}", adminToken);
        assertCodeZero(increaseJson, "admin deposit adjust increase");

        String afterIncreaseJson = httpPost(BASE + "/admin/user/deposit/page",
                "{\"page\":1,\"limit\":1,\"userId\":" + userId + "}", adminToken);
        assertCodeZero(afterIncreaseJson, "admin deposit page after increase");
        assertContains(afterIncreaseJson, "\"availableAmount\"", "调增后应返回可用押金");

        String decreaseJson = httpPost(BASE + "/admin/user/deposit/adjust",
                "{\"userId\":" + userId + ",\"direction\":2,\"amount\":5.00,\"remark\":\"M05调减\"}", adminToken);
        assertCodeZero(decreaseJson, "admin deposit adjust decrease");

        String rejectJson = httpPost(BASE + "/admin/user/deposit/adjust",
                "{\"userId\":" + userId + ",\"direction\":2,\"amount\":999999.00,\"remark\":\"M05超限\"}", adminToken);
        if (rejectJson.contains("\"code\":0")) {
            throw new IllegalStateException("调减超限应拒绝，但返回成功: " + shortText(rejectJson));
        }
    }

    static void assertLoginGuards() throws Exception {
        assertCode401(httpGetRaw(BASE + "/app/user/info"), "app user info 未登录");
        assertCode401(httpGetRaw(BASE + "/app/user/address/list"), "app address list 未登录");
        assertCode401(httpGetRaw(BASE + "/app/user/favorite/list"), "app favorite list 未登录");
        assertCode401(httpGetRaw(BASE + "/app/rental/cart/list"), "app cart list 未登录");
        assertCode401(httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + RENT_START + "\",\"endTime\":\"" + RENT_END + "\",\"qty\":1}", null),
                "app cart add 未登录");
        assertCode401(httpPost(BASE + "/app/rental/order/page", "{\"page\":1,\"limit\":10}", null),
                "app order page 未登录");
    }

    static void assertCode401(String json, String label) {
        if (!json.contains("\"code\":401") && !json.contains("\"code\": 401")) {
            throw new IllegalStateException(label + " 应返回 code=401: " + shortText(json));
        }
    }

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "GET /sqx_fast/health 应返回 UP");
        assertHttpStatus(200, BASE + "/health", "health endpoint");
    }

    static void assertHttpStatus(int expected, String url, String label) throws Exception {
        int status = httpStatus(url);
        if (status != expected) {
            throw new IllegalStateException(label + " HTTP " + status + "，期望 " + expected);
        }
    }

    static int httpStatus(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    static String adminLogin(String uuid, String captcha) throws Exception {
        String body = httpPost(BASE + "/sys/login",
                "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}",
                null);
        assertCodeZero(body, "admin login");
        return extractNested(body, "data", "token");
    }

    static void assertCodeZero(String json, String label) {
        if (!json.contains("\"code\":0") && !json.contains("\"code\": 0")) {
            throw new IllegalStateException(label + " 失败: " + shortText(json));
        }
    }

    static void assertContains(String text, String part, String label) {
        if (text == null || !text.contains(part)) {
            throw new IllegalStateException(label + " 未包含 " + part + ": " + shortText(text));
        }
    }

    static void assertNotContains(String text, String part, String label) {
        if (text != null && text.contains(part)) {
            throw new IllegalStateException(label + " 不应包含 " + part + ": " + shortText(text));
        }
    }

    static void assertFalse(String message, boolean value) {
        if (value) {
            throw new IllegalStateException(message);
        }
    }

    static String loadCaptcha(String uuid) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg")) {
            try (PreparedStatement ps = c.prepareStatement("SELECT code FROM sys_captcha WHERE uuid=?")) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        }
        throw new IllegalStateException("captcha not found for uuid=" + uuid);
    }

    static void httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in != null) {
            in.close();
        }
        conn.disconnect();
    }

    static String httpGetRaw(String url) throws Exception {
        return httpGetRaw(url, null);
    }

    static String httpGetRaw(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        conn.connect();
        return read(conn);
    }

    static String httpGet(String url, String token) throws Exception {
        return httpGetRaw(url, token);
    }

    static String httpPost(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        return read(conn);
    }

    static String httpPut(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("token", token);
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        return read(conn);
    }

    static String read(HttpURLConnection conn) throws Exception {
        InputStream in = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        in.close();
        conn.disconnect();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    static String extractNested(String json, String objectKey, String fieldKey) {
        int objIdx = json.indexOf("\"" + objectKey + "\":");
        if (objIdx < 0) {
            return "";
        }
        return extract(json.substring(objIdx), fieldKey);
    }

    static String extractFirstRecordField(String json, String fieldKey) {
        int recordsIdx = json.indexOf("\"records\":");
        if (recordsIdx < 0) {
            return "";
        }
        int braceIdx = json.indexOf('{', recordsIdx);
        if (braceIdx < 0) {
            return "";
        }
        return extract(json.substring(braceIdx), fieldKey);
    }

    static String extract(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx >= 0) {
            int start = idx + marker.length();
            int end = json.indexOf('"', start);
            return end > start ? json.substring(start, end) : "";
        }
        String numMarker = "\"" + key + "\":";
        idx = json.indexOf(numMarker);
        if (idx < 0) {
            return "";
        }
        int start = idx + numMarker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return end > start ? json.substring(start, end) : "";
    }

    static String shortText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
