package com.sqx.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M01 三端联调冒烟：核对 admin 配置与 uniapp 首页/分类/角标 API 数据一致性。
 * <p>
 * 依赖：server:9135 已启动（含 GET /app/banner/page）、MySQL 种子
 * （rental_v1.sql → m00_integration_seed.sql → m01_tabbar_seed.sql）。
 */
public class M01IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";

    public static void main(String[] args) throws Exception {
        assertHealthOk();
        assertDocOk();

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);

        String adminToken = adminLogin(uuid, captcha);
        String adminBannerJson = httpPost(BASE + "/admin/base/banner/page",
                "{\"page\":1,\"limit\":20,\"classify\":1}", adminToken);
        String adminCategoryJson = httpGet(BASE + "/admin/rental/category/tree", adminToken);
        String adminGoodsJson = httpPost(BASE + "/admin/rental/goods/page",
                "{\"page\":1,\"limit\":20,\"status\":1}", adminToken);

        assertCodeZero(adminBannerJson, "admin banner page");
        assertCodeZero(adminCategoryJson, "admin category tree");
        assertCodeZero(adminGoodsJson, "admin goods page");

        Set<String> adminBannerNames = extractEnabledBannerNames(adminBannerJson);
        Set<String> adminRootCategoryNames = extractRootCategoryNames(adminCategoryJson);
        assertFalse("admin 应存在启用 Banner", adminBannerNames.isEmpty());
        assertFalse("admin 应存在根分类", adminRootCategoryNames.isEmpty());

        String appBannerJson = httpGetRaw(BASE + "/app/banner/page?page=1&limit=20&classify=1");
        String appCategoryJson = httpGetRaw(BASE + "/app/rental/category/tree");
        assertCodeZero(appBannerJson, "uniapp banner page");
        assertCodeZero(appCategoryJson, "uniapp category tree");
        assertHttpStatus(200, BASE + "/app/banner/page?page=1&limit=1&classify=1",
                "uniapp 须使用 GET /app/banner/page（M01 快捷入口）");

        Set<String> appBannerNames = extractRecordFieldValues(appBannerJson, "name");
        Set<String> appRootCategoryNames = extractRootCategoryNames(appCategoryJson);
        assertTrue("uniapp Banner 名称应与 admin 启用项一致",
                appBannerNames.equals(adminBannerNames));
        assertTrue("uniapp 根分类名称应与 admin 一致",
                appRootCategoryNames.equals(adminRootCategoryNames));

        Long categoryId = resolveFirstChildCategoryId(appCategoryJson);
        assertNotNull(categoryId, "分类树应含可用于商品筛选的子分类");
        String goodsBody = "{\"page\":1,\"limit\":10,\"categoryId\":" + categoryId + ",\"rentType\":\"day\",\"status\":1}";
        String appGoodsJson = httpPost(BASE + "/app/rental/goods/page", goodsBody, null);
        assertCodeZero(appGoodsJson, "uniapp goods page by category");
        assertContains(appGoodsJson, "\"categoryId\":" + categoryId,
                "商品列表应按 categoryId 筛选");

        String hourGoodsJson = httpPost(BASE + "/app/rental/goods/page",
                "{\"page\":1,\"limit\":20,\"rentType\":\"hour\",\"status\":1}", null);
        assertCodeZero(hourGoodsJson, "uniapp goods rentType=hour");
        assertContains(hourGoodsJson, "rentPrice", "小时租商品应含 rentPrice");

        String appToken = appLogin();
        String statusJson = httpGet(BASE + "/app/rental/order/status-counts", appToken);
        assertCodeZero(statusJson, "uniapp order status-counts");
        assertOrderBadges(statusJson);

        System.out.println("m01-integration-ok");
    }

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "GET /sqx_fast/health 应返回 UP");
        assertHttpStatus(200, BASE + "/health", "health endpoint");
    }

    static void assertDocOk() throws Exception {
        assertHttpStatus(200, BASE + "/doc.html", "Knife4j doc.html");
    }

    static String adminLogin(String uuid, String captcha) throws Exception {
        String body = httpPost(BASE + "/sys/login",
                "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}",
                null);
        assertCodeZero(body, "admin login");
        String token = extract(body, "token");
        assertFalse("admin token 为空", token.isEmpty());
        return token;
    }

    static String appLogin() throws Exception {
        String body = httpPost(BASE + "/app/base/auth/phonePasswordLogin",
                "{\"phoneNo\":\"13812348888\",\"password\":\"123456\"}", null);
        assertCodeZero(body, "uniapp login camp001");
        String token = extractNested(body, "data", "token");
        assertFalse("app token 为空", token.isEmpty());
        return token;
    }

    static void assertOrderBadges(String json) {
        for (String key : Arrays.asList("pending_payment", "waiting_pickup", "renting", "returning", "after_sale")) {
            int value = extractIntField(json, key);
            assertTrue("角标 " + key + " 应 >= 1（M01 种子）", value >= 1);
        }
    }

    static Set<String> extractEnabledBannerNames(String adminBannerJson) {
        Set<String> names = new LinkedHashSet<>();
        int idx = 0;
        while (true) {
            int rec = adminBannerJson.indexOf("\"records\":[", idx);
            if (rec < 0) {
                break;
            }
            int start = adminBannerJson.indexOf('{', rec);
            int end = findMatchingBrace(adminBannerJson, start);
            if (end < 0) {
                break;
            }
            String chunk = adminBannerJson.substring(start, end + 1);
            if (chunk.contains("\"state\":1")) {
                String name = extract(chunk, "name");
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
            idx = end + 1;
        }
        if (!names.isEmpty()) {
            return names;
        }
        return extractRecordFieldValues(adminBannerJson, "name");
    }

    static Set<String> extractRootCategoryNames(String categoryJson) {
        Set<String> names = new LinkedHashSet<>();
        int idx = 0;
        while (true) {
            int pos = categoryJson.indexOf("\"name\":\"", idx);
            if (pos < 0) {
                break;
            }
            int before = Math.max(0, pos - 120);
            String window = categoryJson.substring(before, pos);
            if (window.contains("\"parentId\":0") || window.contains("\"parentId\": 0")) {
                names.add(extract(categoryJson.substring(pos), "name"));
            }
            idx = pos + 8;
        }
        return names;
    }

    static Set<String> extractRecordFieldValues(String pageJson, String field) {
        Set<String> values = new LinkedHashSet<>();
        String marker = "\"" + field + "\":\"";
        int idx = 0;
        while (true) {
            int pos = pageJson.indexOf(marker, idx);
            if (pos < 0) {
                break;
            }
            int start = pos + marker.length();
            int end = pageJson.indexOf('"', start);
            if (end > start) {
                values.add(pageJson.substring(start, end));
            }
            idx = end + 1;
        }
        return values;
    }

    static Long resolveFirstChildCategoryId(String categoryJson) {
        int childBlock = categoryJson.indexOf("\"children\":[");
        if (childBlock < 0) {
            return null;
        }
        int idPos = categoryJson.indexOf("\"id\":", childBlock);
        if (idPos < 0) {
            return null;
        }
        int start = idPos + 5;
        int end = start;
        while (end < categoryJson.length() && Character.isDigit(categoryJson.charAt(end))) {
            end++;
        }
        return end > start ? Long.parseLong(categoryJson.substring(start, end)) : null;
    }

    static int findMatchingBrace(String text, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    static int extractIntField(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return 0;
        }
        int start = idx + marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    static void assertCodeZero(String json, String label) {
        assertContains(json, "\"code\":0", label + " 应返回 code=0");
    }

    static void assertContains(String text, String needle, String message) {
        if (text == null || !text.contains(needle)) {
            throw new IllegalStateException(message + "，实际片段=" + shortText(text));
        }
    }

    static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    static void assertFalse(String message, boolean condition) {
        assertTrue(message, !condition);
    }

    static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalStateException(message);
        }
    }

    static void assertHttpStatus(int expected, String url, String label) throws Exception {
        int status = httpStatus(url);
        if (status != expected) {
            throw new IllegalStateException(label + " HTTP " + status + "，期望 " + expected);
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
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        return read(conn);
    }

    static String httpGet(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("token", token);
        conn.connect();
        return read(conn);
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

    static String extract(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        int start = idx + marker.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    static String extractNested(String json, String objectKey, String fieldKey) {
        int objIdx = json.indexOf("\"" + objectKey + "\":");
        if (objIdx < 0) {
            return "";
        }
        return extract(json.substring(objIdx), fieldKey);
    }

    static String shortText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
