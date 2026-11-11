package com.sqx.e2e;

import io.restassured.http.ContentType;

import java.net.HttpURLConnection;
import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * M01/M02/M03/M04 E2E 公共工具：server 可达探测、用户端/管理端登录
 */
final class E2eServerSupport {

    /** M03/M04 主链路租期（无 M02 占用冲突） */
    static final String M03_RENT_START = "2026-09-15 10:00:00";
    static final String M03_RENT_END = "2026-09-17 18:00:00";
    /** M04 续租追加天数（对齐 m04_order_lifecycle_seed.sql） */
    static final int M04_EXTRA_DAYS = 1;
    /** M04 续租后结束时间（M03_RENT_END + 1 天） */
    static final String M04_RENT_END_AFTER_RENEW = "2026-09-18 18:00:00";
    /** M02 种子占用窗口（remain=10，qty=11 触发冲突） */
    static final String M02_STOCK_START = "2026-08-10 10:00:00";
    static final String M02_STOCK_END = "2026-08-12 18:00:00";
    static final long M03_GOODS_ID = 1L;
    /** M06 满减券下单租期（7 天租金 > 199 门槛） */
    static final String M06_COUPON_RENT_START = "2026-10-01 10:00:00";
    static final String M06_COUPON_RENT_END = "2026-10-08 18:00:00";
    /** M06 押金冻结专用租期（避开 M02/M03 占用窗口） */
    static final String M06_DEPOSIT_RENT_START = "2026-11-01 10:00:00";
    static final String M06_DEPOSIT_RENT_END = "2026-11-03 18:00:00";
    /** M06 种子券编号 / 押金 / 钱包余额（对齐 m06_user_assets_seed.sql） */
    static final String M06_SEED_COUPON_NO = "CPN-M06-001";
    static final String M06_TEMPLATE_NO = "CTM001";
    static final String M06_SEED_FEEDBACK_NO = "FB-M06-001";
    static final String M06_SEED_DEPOSIT_LOG_NO = "DLOG-M06-001";
    /** M07 商户演示账号（对齐 m07_merchant_seed.sql） */
    static final String MERCHANT_PHONE = System.getenv().getOrDefault(
            "E2E_MERCHANT_PHONE", "13900001001");
    /** M07 主链路租期（避开 M02/M03/M04 占用窗口） */
    static final String M07_RENT_START = "2026-12-01 10:00:00";
    static final String M07_RENT_END = "2026-12-03 18:00:00";
    static final String M07_SEED_MERCHANT_NO = "MCH-SEED-001";

    static final String BASE_URL = System.getenv().getOrDefault(
            "E2E_BASE_URL", "http://localhost:9135/sqx_fast");
    /** 演示手机号（camp001 对应 13812348888） */
    static final String TEST_PHONE = System.getenv().getOrDefault(
            "E2E_APP_PHONE", "13812348888");
    static final String TEST_PASSWORD = System.getenv().getOrDefault(
            "E2E_APP_PASSWORD", "123456");
    static final String ADMIN_USERNAME = System.getenv().getOrDefault(
            "E2E_ADMIN_USERNAME", "admin");
    static final String ADMIN_PASSWORD = System.getenv().getOrDefault(
            "E2E_ADMIN_PASSWORD", "123456");
    static final String MYSQL_JDBC_URL = System.getenv().getOrDefault(
            "E2E_MYSQL_URL", "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false");
    static final String MYSQL_USER = System.getenv().getOrDefault("E2E_MYSQL_USER", "root");
    static final String MYSQL_PASSWORD = System.getenv().getOrDefault("E2E_MYSQL_PASSWORD", "hanstg");
    /** 用户端 JWT 请求头（对齐 sqx.jwt.header=token） */
    static final String APP_TOKEN_HEADER = "token";
    /** 管理端 JWT 请求头 */
    static final String ADMIN_TOKEN_HEADER = "token";

    private E2eServerSupport() {
    }

    static boolean isServerReachable() {
        try {
            String probeUuid = "e2e-probe-" + System.currentTimeMillis();
            URL url = new URL(BASE_URL + "/captcha.jpg?uuid=" + probeUuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status > 0 && status < 500;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M01 TabBar 快捷 Banner 是否已部署（旧 jar 无 GET /app/banner/page） */
    static boolean isM01TabBarApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/banner/page?page=1&limit=1&classify=1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M02 商品浏览接口是否已部署（旧 jar 无 GET /app/rental/search/hot） */
    static boolean isM02GoodsBrowseApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/rental/search/hot");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M03 购物车接口是否已部署（需登录，401 亦表示已注册） */
    static boolean isM03CartOrderApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/rental/cart/list");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M06 用户资产接口是否已部署（需登录，401 亦表示已注册） */
    static boolean isM06UserAssetsApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/user/deposit/summary");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M06 管理端券模板/提现接口是否已部署 */
    static boolean isAdminM06AssetsApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/admin/coupon/template/page");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401 || status == 403;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M05 用户账户接口是否已部署（需登录，401 亦表示已注册） */
    static boolean isM05UserAccountApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/user/favorite/list");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M05 管理端用户列表是否已部署 */
    static boolean isAdminUserApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/admin/user/list");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401 || status == 403;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M04 取货/续租/归还接口是否已部署（需登录，401 亦表示已注册） */
    static boolean isM04OrderLifecycleApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/app/rental/pickup/code/1");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401;
        } catch (Exception ex) {
            return false;
        }
    }

    /** 管理端验收列表是否已部署 */
    static boolean isAdminInspectionApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/admin/rental/inspection/page");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401 || status == 403;
        } catch (Exception ex) {
            return false;
        }
    }

    /** M07 商户端接口是否已部署（需登录；本服务未登录时 HTTP 200 + code 401） */
    static boolean isM07MerchantApiAvailable() {
        return isAppLoginProtectedEndpointReachable("/app/merchant/apply/progress", "GET", null);
    }

    /** M07 管理端商户入驻是否已部署 */
    static boolean isAdminMerchantApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/admin/merchant/apply/page");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401 || status == 403;
        } catch (Exception ex) {
            return false;
        }
    }

    /** 管理端订单列表是否已部署（server Admin 控制器就绪时为 200/401/403） */
    static boolean isAdminOrderApiAvailable() {
        try {
            URL url = new URL(BASE_URL + "/admin/rental/order/page");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getOutputStream().write("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            connection.disconnect();
            return status == 200 || status == 401 || status == 403;
        } catch (Exception ex) {
            return false;
        }
    }

    static boolean isAdminLoginAvailable() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            java.sql.Connection connection = java.sql.DriverManager.getConnection(
                    MYSQL_JDBC_URL, MYSQL_USER, MYSQL_PASSWORD);
            connection.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    static String appLoginToken() {
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                .when()
                .post("/app/base/auth/phonePasswordLogin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    /** M05 登录别名路径（对齐 uniapp 登录页 /app/auth） */
    static String appLoginTokenViaM05Auth() {
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + TEST_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                .when()
                .post("/app/auth/phonePasswordLogin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    /** M07 商户演示账号登录 */
    static String merchantLoginToken() {
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + MERCHANT_PHONE + "\",\"password\":\"" + TEST_PASSWORD + "\"}")
                .when()
                .post("/app/base/auth/phonePasswordLogin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    static String adminLoginToken() {
        String uuid = java.util.UUID.randomUUID().toString();
        given()
                .baseUri(BASE_URL)
                .when()
                .get("/captcha.jpg?uuid=" + uuid)
                .then()
                .statusCode(200);

        String captcha = loadCaptchaCode(uuid);
        return given()
                .baseUri(BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"username\":\"" + ADMIN_USERNAME + "\",\"password\":\"" + ADMIN_PASSWORD
                        + "\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}")
                .when()
                .post("/sys/login")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    private static String loadCaptchaCode(String uuid) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            for (int attempt = 0; attempt < 10; attempt++) {
                try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
                        MYSQL_JDBC_URL, MYSQL_USER, MYSQL_PASSWORD);
                     java.sql.PreparedStatement ps = connection.prepareStatement(
                             "SELECT code FROM sys_captcha WHERE uuid=?")) {
                    ps.setString(1, uuid);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
                Thread.sleep(200L);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("读取验证码被中断 uuid=" + uuid, ex);
        } catch (Exception ex) {
            throw new IllegalStateException("无法从 MySQL 读取验证码，请确认 E2E_MYSQL_* 环境变量", ex);
        }
        throw new IllegalStateException("验证码不存在 uuid=" + uuid);
    }

    /** 探测需 @Login 的 C 端接口：HTTP 200 且 body 含 code:401 亦表示路由已注册 */
    static boolean isAppLoginProtectedEndpointReachable(String path, String method, String body) {
        try {
            URL url = new URL(BASE_URL + path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod(method);
            if (body != null) {
                connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                connection.setDoOutput(true);
                connection.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            int status = connection.getResponseCode();
            java.io.InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = "";
            if (stream != null) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    responseBody = sb.toString();
                }
            }
            connection.disconnect();
            if (status == 401 || status == 403) {
                return true;
            }
            if (status == 200 && responseBody.contains("\"code\":401")) {
                return true;
            }
            if (status == 200 && responseBody.contains("\"code\": 401")) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
}
