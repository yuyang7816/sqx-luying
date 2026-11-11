package com.sqx.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M03 三端联调冒烟：加购→结算预览→下单→模拟支付→待取货；库存冲突；取消待支付。
 * <p>
 * 依赖：server:9135 已启动、MySQL 种子
 * （rental_v1.sql → m00 → m01 → m02 → m03_cart_order_seed.sql）。
 */
public class M03IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String PHONE = "13812348888";
    static final String PASSWORD = "123456";
    static final long GOODS_ID = 1L;
    static final String M03_START = "2026-09-15 10:00:00";
    static final String M03_END = "2026-09-17 18:00:00";
    static final String M02_START = "2026-08-10 10:00:00";
    static final String M02_END = "2026-08-12 18:00:00";

    public static void main(String[] args) throws Exception {
        assertHealthOk();
        String unauthCartJson = httpGetRaw(BASE + "/app/rental/cart/list");
        assertContains(unauthCartJson, "\"code\":401", "uniapp cart list 未登录应返回 code=401");

        String appToken = appLogin();

        String addJson = httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + M03_START + "\",\"endTime\":\"" + M03_END + "\",\"qty\":1}", appToken);
        assertCodeZero(addJson, "cart add");
        long cartId = extractLongField(addJson, "data");

        String listJson = httpGetRaw(BASE + "/app/rental/cart/list", appToken);
        assertCodeZero(listJson, "cart list");
        assertContains(listJson, "\"goodsId\":" + GOODS_ID, "购物车应含商品");

        httpPut(BASE + "/app/rental/cart/select",
                "{\"ids\":[" + cartId + "],\"selected\":true}", appToken);

        String previewJson = httpPost(BASE + "/app/rental/cart/checkout-preview",
                "{\"cartIds\":[" + cartId + "],\"pickupType\":\"store\"}", appToken);
        assertCodeZero(previewJson, "checkout preview");
        assertContains(previewJson, "\"confirmPayload\"", "预览应含 confirmPayload");
        assertContains(previewJson, "\"payAmount\"", "预览应含 payAmount");

        String createBody = buildCreateBodyFromPreview(cartId);
        String createJson = httpPost(BASE + "/app/rental/order/create", createBody, appToken);
        assertCodeZero(createJson, "order create");
        assertContains(createJson, "\"status\":\"pending_payment\"", "新建订单应为 pending_payment");
        String orderNo = extractStringField(createJson, "orderNo");
        long orderId = extractLongField(createJson, "orderId");
        assertFalse("orderNo 为空", orderNo.isEmpty());

        String listAfterJson = httpGetRaw(BASE + "/app/rental/cart/list", appToken);
        assertCodeZero(listAfterJson, "cart list after create");
        assertNotContains(listAfterJson, "\"id\":" + cartId, "下单后购物车条目应清除");

        simulatePaySuccess(orderNo);

        String payStatusJson = httpGetRaw(BASE + "/app/rental/order/pay-status/" + orderNo, appToken);
        assertCodeZero(payStatusJson, "pay status after mock pay");
        assertContains(payStatusJson, "\"status\":\"waiting_pickup\"", "支付后应为 waiting_pickup");
        assertContains(payStatusJson, "\"paid\":true", "支付后 paid=true");
        assertContains(payStatusJson, "\"pickupCode\"", "支付后应有 pickupCode");

        assertOrderInDatabase(orderNo, "waiting_pickup");

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        String adminToken = adminLogin(uuid, captcha);

        String adminPageJson = httpPost(BASE + "/admin/rental/order/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}", adminToken);
        assertCodeZero(adminPageJson, "admin order page");
        assertContains(adminPageJson, "\"orderNo\":\"" + orderNo + "\"", "admin 列表应含订单");
        assertContains(adminPageJson, "\"status\":\"waiting_pickup\"", "admin 列表状态应与 C 端一致");

        String adminDetailJson = httpGetRaw(BASE + "/admin/rental/order/detail/" + orderId, adminToken);
        assertCodeZero(adminDetailJson, "admin order detail");
        assertContains(adminDetailJson, "\"orderNo\":\"" + orderNo + "\"", "admin 详情订单号一致");
        assertContains(adminDetailJson, "\"status\":\"waiting_pickup\"", "admin 详情状态 waiting_pickup");
        assertContains(adminDetailJson, "\"pickupCode\"", "admin 详情应有提货码");
        assertContains(adminDetailJson, "\"contactPhone\":\"" + PHONE + "\"", "admin 详情联系人手机一致");

        String conflictJson = httpPost(BASE + "/app/rental/order/create",
                "{\"items\":[{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\","
                        + "\"startTime\":\"" + M02_START + "\",\"endTime\":\"" + M02_END + "\",\"quantity\":11}],"
                        + "\"pickupType\":\"store\",\"contactName\":\"冲突\",\"contactPhone\":\"" + PHONE + "\"}",
                appToken);
        assertContains(conflictJson, "库存不足", "qty=11 应库存冲突");

        String cancelCreateJson = httpPost(BASE + "/app/rental/order/create",
                buildCreateBodyFromPreview(0), appToken);
        assertCodeZero(cancelCreateJson, "order create for cancel");
        long cancelOrderId = extractLongField(cancelCreateJson, "orderId");
        String cancelOrderNo = extractStringField(cancelCreateJson, "orderNo");

        assertStockOccupyStatus(cancelOrderId, "PRELOCK");

        String cancelJson = httpPost(BASE + "/app/rental/order/cancel/" + cancelOrderId, "{}", appToken);
        assertCodeZero(cancelJson, "cancel pending order");
        assertOrderInDatabase(cancelOrderNo, "cancelled");
        assertStockOccupyStatus(cancelOrderId, "RELEASED");

        System.out.println("m03-integration-ok");
    }

    static String appLogin() throws Exception {
        String body = httpPost(BASE + "/app/base/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + PHONE + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertCodeZero(body, "app login");
        String token = extractNested(body, "data", "token");
        assertFalse("app token 为空", token.isEmpty());
        return token;
    }

    static String buildCreateBodyFromPreview(long cartId) {
        String cartIdsPart = cartId > 0 ? "[" + cartId + "]" : "[]";
        return "{\"items\":[{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                + M03_START + "\",\"endTime\":\"" + M03_END + "\",\"quantity\":1}],"
                + "\"pickupType\":\"store\",\"contactName\":\"冒烟测试\",\"contactPhone\":\""
                + PHONE + "\",\"cartIds\":" + cartIdsPart + "}";
    }

    static void simulatePaySuccess(String orderNo) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg")) {
            c.setAutoCommit(false);
            long orderId;
            String pickupType;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, pickup_type FROM rent_order WHERE order_no=? AND status='pending_payment'")) {
                ps.setString(1, orderNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("待支付订单不存在 " + orderNo);
                    }
                    orderId = rs.getLong("id");
                    pickupType = rs.getString("pickup_type");
                }
            }
            String targetStatus = "delivery".equals(pickupType) ? "delivering" : "waiting_pickup";
            String pickupCode = String.valueOf(100000 + new Random().nextInt(899999));
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE rent_order SET status=?, pickup_code=?, pay_time=NOW() "
                            + "WHERE id=? AND status='pending_payment'")) {
                ps.setString(1, targetStatus);
                ps.setString(2, pickupCode);
                ps.setLong(3, orderId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE rent_stock_occupy SET status='OCCUPIED' WHERE order_id=? AND status='PRELOCK'")) {
                ps.setLong(1, orderId);
                ps.executeUpdate();
            }
            c.commit();
        }
    }

    static void assertOrderInDatabase(String orderNo, String expectedStatus) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement("SELECT status FROM rent_order WHERE order_no=?")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !expectedStatus.equals(rs.getString(1))) {
                    throw new IllegalStateException("订单 " + orderNo + " 状态应为 " + expectedStatus);
                }
            }
        }
    }

    static void assertStockOccupyStatus(long orderId, String expected) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT status FROM rent_stock_occupy WHERE order_id=? ORDER BY id DESC LIMIT 1")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !expected.equals(rs.getString(1))) {
                    throw new IllegalStateException("orderId=" + orderId + " 库存预占应为 " + expected);
                }
            }
        }
    }

    static String adminLogin(String uuid, String captcha) throws Exception {
        String body = httpPost(BASE + "/sys/login",
                "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}",
                null);
        assertCodeZero(body, "admin login");
        String token = extractNested(body, "data", "token");
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

    static void assertCodeZero(String json, String label) {
        assertContains(json, "\"code\":0", label + " 应返回 code=0");
    }

    static void assertContains(String text, String needle, String message) {
        if (text == null || !text.contains(needle)) {
            throw new IllegalStateException(message + "，实际片段=" + shortText(text));
        }
    }

    static void assertNotContains(String text, String needle, String message) {
        if (text != null && text.contains(needle)) {
            throw new IllegalStateException(message + "，实际片段=" + shortText(text));
        }
    }

    static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new IllegalStateException(message);
        }
    }

    static void assertHttpStatus(int expected, String url, String label) throws Exception {
        int status = httpStatus(url);
        if (status != expected) {
            throw new IllegalStateException(label + " HTTP " + status + "，期望 " + expected);
        }
    }

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "GET /sqx_fast/health 应返回 UP");
        assertHttpStatus(200, BASE + "/health", "health endpoint");
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

    static void httpPut(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.flush();
        os.close();
        read(conn);
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

    static long extractLongField(String json, String key) {
        String marker = "\"" + key + "\":";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return 0L;
        }
        int start = idx + marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
            end++;
        }
        return end > start ? Long.parseLong(json.substring(start, end).split("\\.")[0]) : 0L;
    }

    static String extractStringField(String json, String key) {
        return extract(json, key);
    }

    static String shortText(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
