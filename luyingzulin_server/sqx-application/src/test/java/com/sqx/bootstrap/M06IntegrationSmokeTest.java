package com.sqx.bootstrap;

import com.sqx.e2e.E2ePaySimulator;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;

/**
 * M06 三端联调冒烟：优惠券发放→下单核销→admin 可查；提现审核→钱包一致；
 * 押金冻结与订单联动；反馈工单闭环。
 * <p>
 * 依赖：server:9135 已启动；MySQL 已执行 m06_user_assets.sql 与 m06_user_assets_seed.sql。
 */
public class M06IntegrationSmokeTest {

    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String TEST_PHONE = "13812348888";
    static final String TEST_PASSWORD = "123456";
    static final String TEMPLATE_NO = "CTM001";
    static final long GOODS_ID = 1L;
    static final String COUPON_RENT_START = "2026-10-01 10:00:00";
    static final String COUPON_RENT_END = "2026-10-08 18:00:00";
    static final String DEPOSIT_RENT_START = "2026-11-01 10:00:00";
    static final String DEPOSIT_RENT_END = "2026-11-03 18:00:00";

    public static void main(String[] args) throws Exception {
        assertHealthOk();

        String token = appLogin();
        assertAppAssetBaselines(token);

        String adminToken = adminLoginWithCaptcha();

        smokeCouponGrantCheckoutConsume(token, adminToken);
        smokeWithdrawAdminConfirm(token, adminToken);
        smokeFeedbackClosedLoop(token, adminToken);
        smokeDepositFreezeOnPay(token, adminToken);

        System.out.println("m06-integration-ok");
    }

    static void assertHealthOk() throws Exception {
        String body = httpGetRaw(BASE + "/health");
        assertContains(body, "UP", "GET /sqx_fast/health 应返回 UP");
        assertHttpStatus(200, BASE + "/health", "health endpoint");
    }

    static String appLogin() throws Exception {
        String authLoginJson = httpPost(BASE + "/app/auth/phonePasswordLogin",
                "{\"phoneNo\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}", null);
        assertCodeZero(authLoginJson, "app auth login");
        String token = extractNested(authLoginJson, "data", "token");
        assertFalse("登录 token 不应为空", token.isEmpty());
        return token;
    }

    static void assertAppAssetBaselines(String token) throws Exception {
        String couponPageJson = httpPost(BASE + "/app/user/coupon/page",
                "{\"page\":1,\"limit\":10,\"status\":\"unused\"}", token);
        assertCodeZero(couponPageJson, "app coupon page");
        assertContains(couponPageJson, "couponNo", "优惠券列表应含券编号");

        String couponUsableJson = httpGetRaw(
                BASE + "/app/user/coupon/usable?rentAmount=250&deliveryAmount=0&depositAmount=100", token);
        assertCodeZero(couponUsableJson, "app coupon usable");
        assertContains(couponUsableJson, "name", "可用券应含名称");

        String depositSummaryJson = httpGetRaw(BASE + "/app/user/deposit/summary", token);
        assertCodeZero(depositSummaryJson, "app deposit summary");
        assertContains(depositSummaryJson, "totalAmount", "押金概览应含 totalAmount");

        String depositLogJson = httpPost(BASE + "/app/user/deposit/log/page", "{\"page\":1,\"limit\":10}", token);
        assertCodeZero(depositLogJson, "app deposit log page");
        assertContains(depositLogJson, "logType", "押金流水应含类型");

        String walletJson = httpGetRaw(BASE + "/app/userWallet/wallet", token);
        assertCodeZero(walletJson, "app wallet");
        assertContains(walletJson, "amount", "钱包应含余额");

        String withdrawPageJson = httpPost(BASE + "/app/withdrawRecord/page", "{\"page\":1,\"limit\":10}", token);
        assertCodeZero(withdrawPageJson, "app withdraw page");
    }

    static void smokeCouponGrantCheckoutConsume(String token, String adminToken) throws Exception {
        String templatePageJson = httpPost(BASE + "/admin/coupon/template/page",
                "{\"page\":1,\"limit\":10,\"templateNo\":\"" + TEMPLATE_NO + "\"}", adminToken);
        assertCodeZero(templatePageJson, "admin coupon template page");
        assertContains(templatePageJson, "templateNo", "券模板应含 templateNo");
        String templateId = extractFirstRecordField(templatePageJson, "id");
        assertFalse("券模板 id 不应为空", templateId.isEmpty());

        String beforeGrantJson = httpPost(BASE + "/app/user/coupon/page",
                "{\"page\":1,\"limit\":50,\"status\":\"unused\"}", token);
        assertCodeZero(beforeGrantJson, "app coupon page before grant");
        int beforeTotal = parseIntField(beforeGrantJson, "total");

        String grantJson = httpPost(BASE + "/admin/coupon/grant",
                "{\"templateId\":" + templateId + ",\"phones\":[\"" + TEST_PHONE + "\"]}", adminToken);
        assertCodeZero(grantJson, "admin coupon grant");
        assertContains(grantJson, "grantCount", "发券应返回 grantCount");

        String afterGrantJson = httpPost(BASE + "/app/user/coupon/page",
                "{\"page\":1,\"limit\":50,\"status\":\"unused\"}", token);
        assertCodeZero(afterGrantJson, "app coupon page after grant");
        int afterTotal = parseIntField(afterGrantJson, "total");
        if (afterTotal <= beforeTotal) {
            throw new IllegalStateException("发券后未使用券数量应增加");
        }

        String usableJson = httpGetRaw(
                BASE + "/app/user/coupon/usable?rentAmount=250&deliveryAmount=0&depositAmount=100", token);
        assertCodeZero(usableJson, "app coupon usable for checkout");
        String couponId = extractFirstArrayItemField(usableJson, "data", "id");
        assertFalse("可用券 id 不应为空", couponId.isEmpty());

        String cartAddJson = httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + COUPON_RENT_START + "\",\"endTime\":\"" + COUPON_RENT_END + "\",\"qty\":1}", token);
        assertCodeZero(cartAddJson, "app cart add");
        String cartId = extractNested(cartAddJson, "data", "id");
        if (cartId.isEmpty()) {
            cartId = extract(cartAddJson, "data");
        }
        assertFalse("cartId 不应为空", cartId.isEmpty());

        String previewJson = httpPost(BASE + "/app/rental/cart/checkout-preview",
                "{\"cartIds\":[" + cartId + "],\"pickupType\":\"store\",\"couponId\":" + couponId + "}", token);
        assertCodeZero(previewJson, "checkout preview with coupon");
        assertContains(previewJson, "couponAmount", "结算预览应含 couponAmount");

        String createBody = buildCreateOrderBody(previewJson, couponId, cartId, COUPON_RENT_START, COUPON_RENT_END);
        String createJson = httpPost(BASE + "/app/rental/order/create", createBody, token);
        assertCodeZero(createJson, "create order with coupon");
        assertContains(createJson, "pending_payment", "下单后应为 pending_payment");
        String orderNo = extractNested(createJson, "data", "orderNo");
        assertFalse("orderNo 不应为空", orderNo.isEmpty());

        String usedCouponJson = httpPost(BASE + "/app/user/coupon/page",
                "{\"page\":1,\"limit\":50,\"status\":\"used\"}", token);
        assertCodeZero(usedCouponJson, "app used coupon page");
        assertContains(usedCouponJson, orderNo, "已用券应关联 orderNo");

        String orderId = extractNested(createJson, "data", "orderId");
        if (orderId.isEmpty()) {
            orderId = extractFirstRecordField(createJson, "orderId");
        }
        if (!orderId.isEmpty()) {
            httpPost(BASE + "/app/rental/order/cancel/" + orderId, "{}", token);
        }
    }

    static void smokeWithdrawAdminConfirm(String token, String adminToken) throws Exception {
        String walletBeforeJson = httpGetRaw(BASE + "/app/userWallet/wallet", token);
        assertCodeZero(walletBeforeJson, "wallet before withdraw");
        double walletBefore = parseDoubleField(walletBeforeJson, "amount");

        httpPost(BASE + "/app/withdrawalAccount/save",
                "{\"accountName\":\"M06-smoke\",\"accountNumber\":\"m06_smoke@alipay.com\",\"type\":1}", token);

        String accountJson = httpGetRaw(BASE + "/app/withdrawalAccount/getUserWithdrawalAccount?type=1", token);
        assertCodeZero(accountJson, "getUserWithdrawalAccount after save");
        assertContains(accountJson, "M06-smoke", "保存后应能读取提现账户");
        assertContains(accountJson, "m06_smoke@alipay.com", "保存后应返回账号");

        String saveJson = httpPost(BASE + "/app/withdrawRecord/save",
                "{\"amount\":10.00,\"accountType\":2,\"sourceType\":2}", token);
        assertCodeZero(saveJson, "app withdraw save");

        String walletAfterJson = httpGetRaw(BASE + "/app/userWallet/wallet", token);
        assertCodeZero(walletAfterJson, "wallet after withdraw apply");
        double walletAfter = parseDoubleField(walletAfterJson, "amount");
        if (walletBefore - walletAfter < 9.99) {
            throw new IllegalStateException("提现申请应扣减钱包余额");
        }

        String withdrawPageJson = httpPost(BASE + "/app/withdrawRecord/page", "{\"page\":1,\"limit\":5}", token);
        assertCodeZero(withdrawPageJson, "app withdraw page after save");
        String withdrawId = extractFirstRecordField(withdrawPageJson, "id");
        assertFalse("withdrawId 不应为空", withdrawId.isEmpty());

        String adminPageJson = httpPost(BASE + "/admin/withdrawRecord/page", "{\"page\":1,\"limit\":10}", adminToken);
        assertCodeZero(adminPageJson, "admin withdraw page");
        assertContains(adminPageJson, withdrawId, "admin 应能查到提现记录");
        assertContains(adminPageJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 提现记录应返回用户明文手机号");

        String confirmJson = httpPost(BASE + "/admin/withdrawRecord/confirm",
                "{\"id\":" + withdrawId + ",\"status\":2}", adminToken);
        assertCodeZero(confirmJson, "admin withdraw confirm");

        String withdrawAfterJson = httpPost(BASE + "/app/withdrawRecord/page", "{\"page\":1,\"limit\":5}", token);
        assertCodeZero(withdrawAfterJson, "app withdraw page after confirm");
        assertContains(withdrawAfterJson, "\"status\":2", "提现记录应变为已通过");
    }

    static void smokeFeedbackClosedLoop(String token, String adminToken) throws Exception {
        String content = "M06-smoke-feedback-" + System.currentTimeMillis();
        String submitJson = httpPost(BASE + "/app/user/feedback/submit",
                "{\"feedbackType\":\"suggestion\",\"content\":\"" + content
                        + "\",\"contactPhone\":\"" + TEST_PHONE + "\"}", token);
        assertCodeZero(submitJson, "app feedback submit");

        String adminPageJson = httpPost(BASE + "/admin/user/feedback/page",
                "{\"page\":1,\"limit\":20,\"status\":\"pending\"}", adminToken);
        assertCodeZero(adminPageJson, "admin feedback page");
        assertContains(adminPageJson, content, "admin 反馈列表应含新提交内容");
        assertContains(adminPageJson, "feedbackNo", "反馈列表应含 feedbackNo");
        assertContains(adminPageJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 反馈列表应返回用户明文手机号");

        String feedbackId = extractRecordIdByContent(adminPageJson, content);
        assertFalse("feedbackId 不应为空", feedbackId.isEmpty());

        String processJson = httpPost(BASE + "/admin/user/feedback/process",
                "{\"id\":" + feedbackId + ",\"replyRemark\":\"M06-smoke-已处理\"}", adminToken);
        assertCodeZero(processJson, "admin feedback process");

        String infoJson = httpGetRaw(BASE + "/admin/user/feedback/info?id=" + feedbackId, adminToken);
        assertCodeZero(infoJson, "admin feedback info");
        assertContains(infoJson, "processed", "反馈状态应为 processed");
        assertContains(infoJson, "M06-smoke-已处理", "反馈应含处理备注");
        assertContains(infoJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 反馈详情应返回用户明文手机号");
    }

    static void smokeDepositFreezeOnPay(String token, String adminToken) throws Exception {
        String cartAddJson = httpPost(BASE + "/app/rental/cart/add",
                "{\"goodsId\":" + GOODS_ID + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + DEPOSIT_RENT_START + "\",\"endTime\":\"" + DEPOSIT_RENT_END + "\",\"qty\":1}", token);
        assertCodeZero(cartAddJson, "app cart add for deposit");
        String cartId = extractNested(cartAddJson, "data", "id");
        if (cartId.isEmpty()) {
            cartId = extract(cartAddJson, "data");
        }

        String previewJson = httpPost(BASE + "/app/rental/cart/checkout-preview",
                "{\"cartIds\":[" + cartId + "],\"pickupType\":\"store\"}", token);
        assertCodeZero(previewJson, "checkout preview for deposit");
        String createBody = buildCreateOrderBody(previewJson, null, cartId, DEPOSIT_RENT_START, DEPOSIT_RENT_END);
        String createJson = httpPost(BASE + "/app/rental/order/create", createBody, token);
        assertCodeZero(createJson, "create order for deposit");
        String orderNo = extractNested(createJson, "data", "orderNo");
        assertFalse("orderNo 不应为空", orderNo.isEmpty());

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);

        String depositLogJson = httpPost(BASE + "/app/user/deposit/log/page",
                "{\"page\":1,\"limit\":20,\"logType\":\"freeze\",\"orderNo\":\"" + orderNo + "\"}", token);
        assertCodeZero(depositLogJson, "deposit freeze log");
        assertContains(depositLogJson, "freeze", "押金流水应含 freeze");
        assertContains(depositLogJson, orderNo, "押金流水应关联 orderNo");

        String depositSummaryJson = httpGetRaw(BASE + "/app/user/deposit/summary", token);
        assertCodeZero(depositSummaryJson, "deposit summary after pay");
        assertContains(depositSummaryJson, "frozenAmount", "押金概览应含 frozenAmount");

        String depositAdminJson = httpPost(BASE + "/admin/user/deposit/page",
                "{\"page\":1,\"limit\":10,\"phone\":\"" + TEST_PHONE + "\"}", adminToken);
        assertCodeZero(depositAdminJson, "admin deposit page");
        assertContains(depositAdminJson, "\"phone\":\"" + TEST_PHONE + "\"", "admin 押金列表应返回明文手机号");
    }

    static String adminLoginWithCaptcha() throws Exception {
        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        return adminLogin(uuid, captcha);
    }

    static String buildCreateOrderBody(String previewJson, String couponId, String cartId,
            String rentStart, String rentEnd) {
        String pickupType = extractJsonString(previewJson, "pickupType");
        if (pickupType.isEmpty()) {
            pickupType = "store";
        }
        StringBuilder body = new StringBuilder("{\"items\":[{\"goodsId\":").append(GOODS_ID)
                .append(",\"specId\":0,\"rentType\":\"day\",\"startTime\":\"")
                .append(extractFromConfirmPayload(previewJson, "startTime", rentStart))
                .append("\",\"endTime\":\"")
                .append(extractFromConfirmPayload(previewJson, "endTime", rentEnd))
                .append("\",\"quantity\":1}],\"pickupType\":\"").append(pickupType)
                .append("\",\"contactName\":\"M06-smoke\",\"contactPhone\":\"").append(TEST_PHONE)
                .append("\",\"cartIds\":[").append(cartId).append(']');
        if (couponId != null && !couponId.isEmpty()) {
            body.append(",\"couponId\":").append(couponId);
        }
        body.append('}');
        return body.toString();
    }

    static String extractFromConfirmPayload(String json, String field, String fallback) {
        int idx = json.indexOf("\"confirmPayload\"");
        if (idx < 0) {
            return fallback;
        }
        int itemsIdx = json.indexOf("\"items\"", idx);
        if (itemsIdx < 0) {
            return fallback;
        }
        String val = extract(json.substring(itemsIdx), field);
        return val.isEmpty() ? fallback : val;
    }

    static String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        int start = idx + marker.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    static String extractRecordIdByContent(String json, String content) {
        int contentIdx = json.indexOf(content);
        if (contentIdx < 0) {
            return "";
        }
        int recordStart = json.lastIndexOf('{', contentIdx);
        int recordEnd = json.indexOf('}', contentIdx);
        if (recordStart < 0 || recordEnd < 0) {
            return "";
        }
        return extract(json.substring(recordStart, recordEnd + 1), "id");
    }

    static int parseIntField(String json, String key) {
        String numMarker = "\"" + key + "\":";
        int idx = json.indexOf(numMarker);
        if (idx < 0) {
            return 0;
        }
        int start = idx + numMarker.length();
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

    static double parseDoubleField(String json, String key) {
        String numMarker = "\"" + key + "\":";
        int idx = json.indexOf(numMarker);
        if (idx < 0) {
            return 0;
        }
        int start = idx + numMarker.length();
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-' || json.charAt(end) == '.')) {
            end++;
        }
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    static String extractFirstArrayItemField(String json, String arrayKey, String fieldKey) {
        int arrayIdx = json.indexOf("\"" + arrayKey + "\":");
        if (arrayIdx < 0) {
            return "";
        }
        int braceIdx = json.indexOf('{', arrayIdx);
        if (braceIdx < 0) {
            return "";
        }
        int braceEnd = json.indexOf('}', braceIdx);
        if (braceEnd < 0) {
            return "";
        }
        return extract(json.substring(braceIdx, braceEnd + 1), fieldKey);
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
                "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha
                        + "\"}", null);
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

    static void assertFalse(String message, boolean value) {
        if (value) {
            throw new IllegalStateException(message);
        }
    }

    static String loadCaptcha(String uuid) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
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
        throw new IllegalStateException("captcha not found");
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

    static String extractFirstRecordField(String json, String fieldKey) {
        int recordsIdx = json.indexOf("\"records\":");
        if (recordsIdx < 0) {
            return extract(json, fieldKey);
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
