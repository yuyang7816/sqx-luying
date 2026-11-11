package com.sqx.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M02 三端联调冒烟：搜索→列表→详情→库存校验；管理端新增商品→上架→uniapp 可见。
 * <p>
 * 依赖：server:9135 已启动、MySQL 种子
 * （rental_v1.sql → m00_integration_seed.sql → m01_tabbar_seed.sql → m02_goods_browse_seed.sql）。
 */
public class M02IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String KEYWORD_TENT = "帐篷";
    static final String STOCK_START = "2026-08-10 10:00:00";
    static final String STOCK_END = "2026-08-12 18:00:00";

    public static void main(String[] args) throws Exception {
        assertHealthOk();

        assertHttpStatus(200, BASE + "/app/rental/search/hot", "uniapp search hot");

        String hotJson = httpGetRaw(BASE + "/app/rental/search/hot");
        assertCodeZero(hotJson, "uniapp search hot");
        assertContains(hotJson, KEYWORD_TENT, "热门词应含「帐篷」");

        String listJson = httpPost(BASE + "/app/rental/goods/page",
                "{\"page\":1,\"limit\":10,\"keyword\":\"" + KEYWORD_TENT + "\",\"rentType\":\"day\"}", null);
        assertCodeZero(listJson, "uniapp goods page keyword");
        assertContains(listJson, "\"goodsNo\"", "列表应含 goodsNo");

        String detailJson = httpGetRaw(BASE + "/app/rental/goods/detail/1");
        assertCodeZero(detailJson, "uniapp goods detail");
        assertContains(detailJson, "地钉防风绳加强包", "详情应含 M02 套餐");
        assertContains(detailJson, "容纳人数", "详情应含规格");

        String stockOkJson = httpPost(BASE + "/app/rental/stock/check",
                "{\"goodsId\":1,\"specId\":0,\"startTime\":\"" + STOCK_START
                        + "\",\"endTime\":\"" + STOCK_END + "\",\"quantity\":10}", null);
        assertCodeZero(stockOkJson, "stock check available");
        assertContains(stockOkJson, "\"available\":true", "qty=10 应可用");
        assertContains(stockOkJson, "\"remainStock\":10", "剩余库存应为 10");

        String stockConflictJson = httpPost(BASE + "/app/rental/stock/check",
                "{\"goodsId\":1,\"specId\":0,\"startTime\":\"" + STOCK_START
                        + "\",\"endTime\":\"" + STOCK_END + "\",\"quantity\":11}", null);
        assertCodeZero(stockConflictJson, "stock check conflict");
        assertContains(stockConflictJson, "\"available\":false", "qty=11 应冲突");
        assertContains(stockConflictJson, "\"remainStock\":10", "冲突时仍返回 remainStock");

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        String adminToken = adminLogin(uuid, captcha);

        String adminCategoryJson = httpGetRaw(BASE + "/admin/rental/category/tree", adminToken);
        String appCategoryJson = httpGetRaw(BASE + "/app/rental/category/tree");
        assertCodeZero(adminCategoryJson, "admin category tree");
        assertCodeZero(appCategoryJson, "uniapp category tree");
        assertContains(appCategoryJson, extractFirstRootCategoryName(adminCategoryJson),
                "uniapp 分类树应与 admin 根分类一致");

        String uniqueName = "SMOKE-M02-" + System.currentTimeMillis();
        String uniqueGoodsNo = "RG-SMOKE-M02-" + System.currentTimeMillis();
        String saveBody = "{\"goodsNo\":\"" + uniqueGoodsNo + "\",\"name\":\"" + uniqueName
                + "\",\"categoryId\":11,\"rentPriceDay\":19.90,\"depositAmount\":100.00,"
                + "\"totalStock\":5,\"status\":0,"
                + "\"coverImage\":\"/upload/m02/smoke-cover.jpg\","
                + "\"images\":[\"/upload/m02/smoke-1.jpg\",\"/upload/m02/smoke-2.jpg\"],"
                + "\"specs\":[{\"specName\":\"容纳人数\",\"specValue\":\"2-3人\",\"sort\":1}],"
                + "\"packages\":[{\"packageName\":\"联调测试套餐\",\"packageDesc\":\"M02 冒烟\",\"price\":8.80,\"sort\":1}]}";
        httpPost(BASE + "/admin/rental/goods/save", saveBody, adminToken);

        String adminPageJson = httpPost(BASE + "/admin/rental/goods/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + uniqueName + "\"}", adminToken);
        assertCodeZero(adminPageJson, "admin goods page after save");
        assertContains(adminPageJson, uniqueName, "admin 应能查到新建商品");

        Long goodsId = extractFirstRecordLongField(adminPageJson, "id");
        assertNotNull(goodsId, "新建商品应有 id");

        String detailBeforeShelf = httpGetRaw(BASE + "/app/rental/goods/detail/" + goodsId);
        assertCodeZero(detailBeforeShelf, "下架态详情");
        assertContains(detailBeforeShelf, "联调测试套餐", "详情应含 admin 配置套餐");
        assertContains(detailBeforeShelf, "容纳人数", "详情应含 admin 配置规格");

        httpPost(BASE + "/admin/rental/goods/updateState",
                "{\"id\":" + goodsId + ",\"status\":1}", adminToken);

        String appListJson = httpPost(BASE + "/app/rental/goods/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + uniqueName + "\",\"rentType\":\"day\",\"status\":1}", null);
        assertCodeZero(appListJson, "uniapp goods page after on-shelf");
        assertContains(appListJson, uniqueGoodsNo, "上架后 uniapp 列表应可见");

        httpPost(BASE + "/admin/rental/goods/updateState",
                "{\"id\":" + goodsId + ",\"status\":0}", adminToken);

        String appListOffJson = httpPost(BASE + "/app/rental/goods/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + uniqueName + "\",\"rentType\":\"day\",\"status\":1}", null);
        assertCodeZero(appListOffJson, "uniapp goods page after off-shelf");
        assertNotContains(appListOffJson, uniqueGoodsNo, "下架后 uniapp 上架列表不应出现");

        httpPost(BASE + "/admin/rental/goods/delete", "{\"id\":" + goodsId + "}", adminToken);

        System.out.println("m02-integration-ok");
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

    static Long extractFirstRecordLongField(String pageJson, String field) {
        String marker = "\"" + field + "\":";
        int rec = pageJson.indexOf("\"records\":[");
        if (rec < 0) {
            return null;
        }
        int idx = pageJson.indexOf(marker, rec);
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        int end = start;
        while (end < pageJson.length() && Character.isDigit(pageJson.charAt(end))) {
            end++;
        }
        return end > start ? Long.parseLong(pageJson.substring(start, end)) : null;
    }

    static void assertCodeZero(String json, String label) {
        assertContains(json, "\"code\":0", label + " 应返回 code=0");
    }

    static void assertContains(String text, String needle, String message) {
        if (text == null || !text.contains(needle)) {
            throw new IllegalStateException(message + "，实际片段=" + shortText(text));
        }
    }

    static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new IllegalStateException(message);
        }
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

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "GET /sqx_fast/health 应返回 UP");
        assertHttpStatus(200, BASE + "/health", "health endpoint");
    }

    static String extractFirstRootCategoryName(String categoryJson) {
        int pos = categoryJson.indexOf("\"name\":\"");
        if (pos < 0) {
            return "";
        }
        int start = pos + 8;
        int end = categoryJson.indexOf('"', start);
        return end > start ? categoryJson.substring(start, end) : "";
    }

    static void assertNotContains(String text, String needle, String message) {
        if (text != null && text.contains(needle)) {
            throw new IllegalStateException(message + "，实际片段=" + shortText(text));
        }
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

    static String shortText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
