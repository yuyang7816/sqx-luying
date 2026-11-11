package com.sqx.bootstrap;

import com.sqx.e2e.E2ePaySimulator;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M04 三端联调冒烟：M03 下单支付→取货→续租→归还→验收完成。
 * <p>
 * 依赖：server:9135 已启动、MySQL 种子链至 m04_order_lifecycle.sql。
 */
public class M04IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String PHONE = "13812348888";
    static final String PASSWORD = "123456";
    static final long GOODS_ID = 1L;
    static final String M03_START = "2026-09-15 10:00:00";
    static final String M03_END = "2026-09-17 18:00:00";
    static final String M04_END_AFTER_RENEW = "2026-09-18 18:00:00";
    static final int EXTRA_DAYS = 1;

    public static void main(String[] args) throws Exception {
        assertHealthOk();
        String appToken = appLogin();

        long cartId = addCart(appToken);
        String createJson = httpPost(BASE + "/app/rental/order/create", buildCreateBody(cartId), appToken);
        assertCodeZero(createJson, "order create");
        String orderNo = extractStringField(createJson, "orderNo");
        long orderId = extractLongField(createJson, "orderId");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        assertOrderInDatabase(orderNo, "waiting_pickup");

        String pickupCodeJson = httpGetRaw(BASE + "/app/rental/pickup/code/" + orderId, appToken);
        assertCodeZero(pickupCodeJson, "pickup code");
        assertContains(pickupCodeJson, "\"pickupCode\"", "应有取货码");

        String confirmJson = httpPost(BASE + "/app/rental/pickup/confirm/" + orderId, "{}", appToken);
        assertCodeZero(confirmJson, "confirm pickup");
        assertOrderInDatabase(orderNo, "renting");

        String renewPreviewJson = httpGetRaw(
                BASE + "/app/rental/renew/preview/" + orderId
                        + "?newEndTime=" + M04_END_AFTER_RENEW.replace(" ", "%20"), appToken);
        assertCodeZero(renewPreviewJson, "renew preview");
        assertContains(renewPreviewJson, "\"extraHours\"", "续租应返回计费小时数");

        String renewCreateJson = httpPost(BASE + "/app/rental/renew/create",
                "{\"orderId\":" + orderId + ",\"newEndTime\":\"" + M04_END_AFTER_RENEW + "\"}", appToken);
        assertCodeZero(renewCreateJson, "renew create");
        String renewNo = extractStringField(renewCreateJson, "renewNo");
        assertFalse("renewNo 为空", renewNo.isEmpty());

        E2ePaySimulator.simulateRenewPaySuccess(renewNo);
        assertRenewPaid(renewNo);
        assertOrderEndTime(orderNo, M04_END_AFTER_RENEW);

        String returnJson = httpPost(BASE + "/app/rental/return/apply",
                "{\"orderId\":" + orderId + ",\"returnMethod\":\"store\",\"returnDate\":\"2026-09-18\"}",
                appToken);
        assertCodeZero(returnJson, "return apply");
        assertOrderInDatabase(orderNo, "returning");

        String returnDetailJson = httpGetRaw(BASE + "/app/rental/return/detail/" + orderId, appToken);
        assertCodeZero(returnDetailJson, "return detail");
        assertContains(returnDetailJson, "\"returnMethod\":\"store\"", "归还方式一致");

        String inspectJson = httpPost(BASE + "/app/merchant/inspection/submit",
                "{\"orderId\":" + orderId + ",\"deviceCondition\":\"good\",\"deductAmount\":0}", appToken);
        assertCodeZero(inspectJson, "inspection submit");
        assertOrderInDatabase(orderNo, "completed");
        assertInspectionRefundExists(orderNo);

        String appDetailJson = httpGetRaw(BASE + "/app/rental/order/detail/" + orderId, appToken);
        assertCodeZero(appDetailJson, "app order detail");
        assertContains(appDetailJson, "\"status\":\"completed\"", "用户端详情 completed");
        assertContains(appDetailJson, "\"logs\"", "用户端应有时间轴");
        assertContains(appDetailJson, "\"renewRecords\"", "用户端应有续租记录");

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String adminToken = adminLogin(uuid, loadCaptcha(uuid));

        String adminPageJson = httpPost(BASE + "/admin/rental/order/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}", adminToken);
        assertCodeZero(adminPageJson, "admin order page");
        assertContains(adminPageJson, "\"status\":\"completed\"", "admin 列表状态 completed");

        String adminDetailJson = httpGetRaw(BASE + "/admin/rental/order/detail/" + orderId, adminToken);
        assertCodeZero(adminDetailJson, "admin order detail");
        assertContains(adminDetailJson, "\"status\":\"completed\"", "admin 详情状态与 C 端一致");
        assertContains(adminDetailJson, "\"logs\"", "admin 详情应有时间轴");

        String adminInspectPageJson = httpPost(BASE + "/admin/rental/inspection/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}", adminToken);
        assertCodeZero(adminInspectPageJson, "admin inspection page");
        assertContains(adminInspectPageJson, "\"orderNo\":\"" + orderNo + "\"", "admin 验收列表可查退押");
        assertContains(adminInspectPageJson, "\"refundAmount\"", "admin 验收列表应含退押金额");

        assertOrderLogComplete(orderNo);

        System.out.println("m04-integration-ok");
    }

    static String appLogin() throws Exception {
        String body = httpPost(BASE + "/app/base/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + PHONE + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertCodeZero(body, "app login");
        return extractNested(body, "data", "token");
    }

    static long addCart(String appToken) throws Exception {
        String addJson = httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + M03_START + "\",\"endTime\":\"" + M03_END + "\",\"qty\":1}", appToken);
        assertCodeZero(addJson, "cart add");
        return extractLongField(addJson, "data");
    }

    static String buildCreateBody(long cartId) {
        return "{\"items\":[{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                + M03_START + "\",\"endTime\":\"" + M03_END + "\",\"quantity\":1}],"
                + "\"pickupType\":\"store\",\"contactName\":\"M04冒烟\",\"contactPhone\":\""
                + PHONE + "\",\"cartIds\":[" + cartId + "]}";
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

    static void assertRenewPaid(String renewNo) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT pay_status FROM rent_renew_record WHERE renew_no=?")) {
            ps.setString(1, renewNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"success".equals(rs.getString(1))) {
                    throw new IllegalStateException("续租单 " + renewNo + " 应已支付");
                }
            }
        }
    }

    static void assertOrderEndTime(String orderNo, String expected) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT DATE_FORMAT(end_time,'%Y-%m-%d %H:%i:%s') FROM rent_order WHERE order_no=?")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !expected.equals(rs.getString(1))) {
                    throw new IllegalStateException("订单结束时间应为 " + expected);
                }
            }
        }
    }

    static void assertInspectionRefundExists(String orderNo) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT refund_amount FROM rent_inspection WHERE order_no=?")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getBigDecimal(1) == null) {
                    throw new IllegalStateException("验收退押记录不存在 orderNo=" + orderNo);
                }
            }
        }
    }

    /** 校验 rent_order_log 覆盖支付→取货→续租→归还→验收关键节点 */
    static void assertOrderLogComplete(String orderNo) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT to_status, COUNT(*) AS cnt FROM rent_order_log WHERE order_no=? "
                             + "GROUP BY to_status")) {
            ps.setString(1, orderNo);
            java.util.Set<String> seen = new java.util.HashSet<>();
            int total = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    seen.add(rs.getString("to_status"));
                    total += rs.getInt("cnt");
                }
            }
            if (total < 3) {
                throw new IllegalStateException("rent_order_log 条目过少 orderNo=" + orderNo + " total=" + total);
            }
            for (String required : new String[]{"waiting_pickup", "renting", "returning", "completed"}) {
                if (!seen.contains(required)) {
                    throw new IllegalStateException("rent_order_log 缺少状态 " + required + " orderNo=" + orderNo);
                }
            }
        }
    }

    static String adminLogin(String uuid, String captcha) throws Exception {
        String body = httpPost(BASE + "/sys/login",
                "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}",
                null);
        assertCodeZero(body, "admin login");
        return extractNested(body, "data", "token");
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
        throw new IllegalStateException("captcha not found uuid=" + uuid);
    }

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "health UP");
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
        assertContains(json, "\"code\":0", label + " code=0");
    }

    static void assertContains(String text, String needle, String message) {
        if (text == null || !text.contains(needle)) {
            throw new IllegalStateException(message + "，片段=" + shortText(text));
        }
    }

    static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new IllegalStateException(message);
        }
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

    static String httpGetRaw(String url) throws Exception {
        return httpGetRaw(url, null);
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
