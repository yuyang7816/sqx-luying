package com.sqx.bootstrap;

import com.sqx.e2e.E2ePaySimulator;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * M07 三端联调冒烟：商户种子→C 端下单支付→LINK 同步→备货核销→归还验收→admin 可查。
 * <p>
 * 依赖：server:9135 已启动、MySQL 种子链至 m07_merchant_seed.sql。
 * 执行：mvn -q -pl sqx-application exec:java -Dexec.mainClass=com.sqx.bootstrap.M07IntegrationSmokeTest
 */
public class M07IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String C_PHONE = "13812348888";
    static final String M_PHONE = "13900001001";
    static final String PASSWORD = "123456";
    static final long GOODS_ID = 1L;
    static final String START = "2026-12-05 10:00:00";
    static final String END = "2026-12-07 18:00:00";

    public static void main(String[] args) throws Exception {
        assertHealthOk();
        com.sqx.e2e.E2eM07SeedSupport.ensureMerchantSeedIfMissing();

        String merchantToken = appLogin(M_PHONE);
        String progressJson = httpGetRaw(BASE + "/app/merchant/apply/progress", merchantToken);
        assertCodeZero(progressJson, "merchant progress");
        assertContains(progressJson, "\"status\":\"approved\"", "种子商户应已审核通过");

        String goodsPageJson = httpPost(BASE + "/app/merchant/goods/page",
                "{\"page\":1,\"limit\":10}", merchantToken);
        assertCodeZero(goodsPageJson, "merchant goods page");
        assertContains(goodsPageJson, "\"records\"", "商户商品列表");

        String storeJson = httpGetRaw(BASE + "/app/merchant/store/detail", merchantToken);
        assertCodeZero(storeJson, "merchant store detail");
        assertContains(storeJson, "\"staffList\"", "门店应含员工列表");

        String staffListJson = httpGetRaw(BASE + "/app/merchant/staff/list", merchantToken);
        assertCodeZero(staffListJson, "merchant staff list");

        String cToken = appLogin(C_PHONE);
        long cartId = addCart(cToken);
        String createJson = httpPost(BASE + "/app/rental/order/create", buildCreateBody(cartId), cToken);
        assertCodeZero(createJson, "order create");
        String orderNo = extractStringField(createJson, "orderNo");
        long orderId = extractLongField(createJson, "orderId");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        assertOrderInDatabase(orderNo, "waiting_pickup");

        String merchantOrderPageJson = httpPost(BASE + "/app/merchant/order/page",
                "{\"page\":1,\"limit\":20,\"tab\":\"pending\"}", merchantToken);
        assertCodeZero(merchantOrderPageJson, "merchant order page");
        assertContains(merchantOrderPageJson, "LINK-" + orderId, "支付后应同步 LINK 订单");

        String linkId = "LINK-" + orderId;
        String prepareJson = httpPost(BASE + "/app/merchant/order/prepare/" + linkId, "{}", merchantToken);
        assertCodeZero(prepareJson, "merchant prepare");

        String pickupCodeJson = httpGetRaw(BASE + "/app/rental/pickup/code/" + orderId, cToken);
        assertCodeZero(pickupCodeJson, "pickup code");
        String pickupCode = extractStringField(pickupCodeJson, "pickupCode");

        String pickupJson = httpPost(BASE + "/app/merchant/order/pickup/" + linkId,
                "{\"pickupCode\":\"" + pickupCode + "\"}", merchantToken);
        assertCodeZero(pickupJson, "merchant pickup");
        assertOrderInDatabase(orderNo, "renting");

        String contactJson = httpGetRaw(BASE + "/app/merchant/order/contact/" + linkId, merchantToken);
        assertCodeZero(contactJson, "merchant contact");
        assertContains(contactJson, "\"contactPhone\"", "应返回联系人手机号");
        assertContains(contactJson, "\"userId\"", "应返回用户ID供 IM 联系");

        long customerUserId = extractLongField(contactJson, "userId");
        String merchantProgressJson = httpGetRaw(BASE + "/app/merchant/apply/progress", merchantToken);
        long merchantId = extractLongField(merchantProgressJson, "merchantId");

        String openImJson = httpGetRaw(BASE + "/app/im/conversation/open/" + merchantId, cToken);
        assertCodeZero(openImJson, "user open im");
        long conversationId = extractLongField(openImJson, "id");

        String userSendJson = httpPost(BASE + "/app/im/message/send",
                "{\"conversationId\":" + conversationId + ",\"content\":\"M07冒烟咨询\"}", cToken);
        assertCodeZero(userSendJson, "user im send");

        String merchantUnreadJson = httpPost(BASE + "/app/merchant/messages/unread-summary", "{}", merchantToken);
        assertCodeZero(merchantUnreadJson, "merchant unread summary");
        assertContains(merchantUnreadJson, "\"imUnread\"", "商户 IM 未读汇总");

        String merchantConvJson = httpPost(BASE + "/app/merchant/im/conversation/page",
                "{\"page\":1,\"limit\":10}", merchantToken);
        assertCodeZero(merchantConvJson, "merchant im conversation page");

        String merchantOpenJson = httpGetRaw(BASE + "/app/merchant/im/conversation/open/" + customerUserId, merchantToken);
        assertCodeZero(merchantOpenJson, "merchant open im");

        String merchantSendJson = httpPost(BASE + "/app/merchant/im/message/send",
                "{\"conversationId\":" + conversationId + ",\"content\":\"M07商户回复\"}", merchantToken);
        assertCodeZero(merchantSendJson, "merchant im send");

        String subscribeJson = httpPost(BASE + "/app/messages/subscribe-templates", "{}", cToken);
        assertCodeZero(subscribeJson, "subscribe templates");
        assertContains(subscribeJson, "\"orderNotify\"", "应返回订阅模板配置");

        String returnJson = httpPost(BASE + "/app/rental/return/apply",
                "{\"orderId\":" + orderId + ",\"returnMethod\":\"store\",\"returnDate\":\"2026-12-07\"}",
                cToken);
        assertCodeZero(returnJson, "return apply");
        assertOrderInDatabase(orderNo, "returning");

        String inspectJson = httpPost(BASE + "/app/merchant/inspection/submit",
                "{\"orderId\":" + orderId + ",\"deviceCondition\":\"good\",\"deductAmount\":0,"
                        + "\"remark\":\"M07-integration\"}", merchantToken);
        assertCodeZero(inspectJson, "merchant inspection");
        assertOrderInDatabase(orderNo, "completed");
        assertInspectionRefundExists(orderNo);

        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String adminToken = adminLogin(uuid, loadCaptcha(uuid));
        String adminApplyJson = httpPost(BASE + "/admin/merchant/apply/page",
                "{\"page\":1,\"limit\":20}", adminToken);
        assertCodeZero(adminApplyJson, "admin apply page");

        String adminOrderJson = httpPost(BASE + "/admin/rental/order/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}", adminToken);
        assertCodeZero(adminOrderJson, "admin order page");
        assertContains(adminOrderJson, "\"status\":\"completed\"", "admin 订单状态 completed");

        String adminInspectJson = httpPost(BASE + "/admin/rental/inspection/page",
                "{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}", adminToken);
        assertCodeZero(adminInspectJson, "admin inspection page");
        assertContains(adminInspectJson, orderNo, "admin 验收记录可见");

        System.out.println("m07-integration-ok");
    }

    static String appLogin(String phone) throws Exception {
        String body = httpPost(BASE + "/app/base/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + phone + "\",\"password\":\"" + PASSWORD + "\"}", null);
        assertCodeZero(body, "app login " + phone);
        return extractNested(body, "data", "token");
    }

    static long addCart(String appToken) throws Exception {
        String addJson = httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + START + "\",\"endTime\":\"" + END + "\",\"qty\":1}", appToken);
        assertCodeZero(addJson, "cart add");
        return extractLongField(addJson, "data");
    }

    static String buildCreateBody(long cartId) {
        return "{\"items\":[{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                + START + "\",\"endTime\":\"" + END + "\",\"quantity\":1}],"
                + "\"pickupType\":\"store\",\"contactName\":\"M07冒烟\",\"contactPhone\":\""
                + C_PHONE + "\",\"cartIds\":[" + cartId + "]}";
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

    static void assertInspectionRefundExists(String orderNo) throws Exception {
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT refund_amount FROM rent_inspection WHERE order_no=? ORDER BY id DESC LIMIT 1")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("验收记录不存在 orderNo=" + orderNo);
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
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false", "root", "hanstg");
             PreparedStatement ps = c.prepareStatement("SELECT code FROM sys_captcha WHERE uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return "8888";
    }

    static void assertHealthOk() throws Exception {
        String json = httpGetRaw(BASE + "/health", null);
        if (!json.contains("UP") && !json.contains("\"code\":0")) {
            throw new IllegalStateException("health 检查失败: " + json);
        }
    }

    static void assertCodeZero(String json, String step) {
        if (json == null || (!json.contains("\"code\":0") && !json.contains("\"code\": 0"))) {
            throw new IllegalStateException(step + " 失败: " + json);
        }
    }

    static void assertContains(String json, String needle, String msg) {
        if (json == null || !json.contains(needle)) {
            throw new IllegalStateException(msg + "，未找到: " + needle + " in " + json);
        }
    }

    static String httpGet(String url) throws Exception {
        httpGetRaw(url, null);
        return "";
    }

    static String httpGetRaw(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        return readResponse(conn);
    }

    static String httpPost(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    static String readResponse(HttpURLConnection conn) throws Exception {
        InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (is == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    static String extractNested(String json, String... keys) {
        String current = json;
        for (String key : keys) {
            current = extractStringField(current, key);
        }
        return current;
    }

    static String extractStringField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) {
            pattern = "\"" + field + "\":";
            start = json.indexOf(pattern);
            if (start < 0) {
                return "";
            }
            start += pattern.length();
            while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
                start++;
            }
            if (start < json.length() && json.charAt(start) == '"') {
                start++;
                int end = json.indexOf('"', start);
                return end > start ? json.substring(start, end) : "";
            }
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end).trim();
        }
        start += pattern.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    static long extractLongField(String json, String field) {
        String val = extractStringField(json, field);
        if (val.isEmpty()) {
            throw new IllegalStateException("字段 " + field + " 未找到: " + json);
        }
        return Long.parseLong(val);
    }
}
