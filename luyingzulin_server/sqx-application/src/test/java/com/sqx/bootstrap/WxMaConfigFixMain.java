package com.sqx.bootstrap;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import java.util.UUID;

/**
 * 对齐小程序登录 AppId/Secret 与 manifest.json，并刷新 Redis 配置缓存。
 */
public class WxMaConfigFixMain {
    private static final String JDBC = "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "hanstg";
    private static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    private static final String MA_APP_ID = "wx3c3a5eb165c4fcba";
    private static final String MA_SECRET = "bccc6038ba19e5f1e3009ba151dffa95";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(JDBC, DB_USER, DB_PASS)) {
            printCurrentValues(connection);
            applyFix(connection);
            printCurrentValues(connection);
        }
        refreshConfigCache();
        System.out.println("wx-ma-config-fix-ok");
    }

    private static void printCurrentValues(Connection connection) throws Exception {
        String sql = "SELECT param_key, param_value FROM sys_config "
                + "WHERE param_key IN ("
                + "'sys_user_ma_appid', 'sys_user_ma_secret', 'sys_wechat_jsapi_appid', "
                + "'sys_wechat_jsapi_secret', 'pay_wx_jsapi_app_id'"
                + ") ORDER BY param_key";
        System.out.println("---- current sys_config ----");
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println(rs.getString("param_key") + " = " + rs.getString("param_value"));
            }
        }
    }

    private static void applyFix(Connection connection) throws Exception {
        updateConfig(connection, "sys_user_ma_appid", MA_APP_ID);
        updateConfig(connection, "sys_user_ma_secret", MA_SECRET);
        updateConfig(connection, "sys_wechat_jsapi_appid", MA_APP_ID);
        updateConfig(connection, "sys_wechat_jsapi_secret", MA_SECRET);
        updateConfig(connection, "pay_wx_jsapi_app_id", MA_APP_ID);
    }

    private static void updateConfig(Connection connection, String key, String value) throws Exception {
        String sql = "UPDATE sys_config SET param_value = ? WHERE param_key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int updated = ps.executeUpdate();
            System.out.println("update " + key + " -> " + value + " (rows=" + updated + ")");
        }
    }

    private static void refreshConfigCache() throws Exception {
        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        String loginBody = "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}";
        String loginResp = httpPost(BASE + "/sys/login", loginBody, null);
        String token = extract(loginResp, "token");
        if (token.isEmpty()) {
            throw new IllegalStateException("admin login failed: " + loginResp);
        }
        String refreshResp = httpGet(BASE + "/sys/config/refresh", token);
        System.out.println("config refresh: " + refreshResp);
    }

    private static String loadCaptcha(String uuid) throws Exception {
        try (Connection connection = DriverManager.getConnection(JDBC, DB_USER, DB_PASS);
             PreparedStatement ps = connection.prepareStatement("SELECT code FROM sys_captcha WHERE uuid = ? LIMIT 1")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code");
                }
            }
        }
        throw new IllegalStateException("captcha not found for uuid=" + uuid);
    }

    private static String httpGet(String url, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("token", token);
        }
        conn.connect();
        String body = readBody(conn);
        conn.disconnect();
        return body;
    }

    private static String httpGet(String url) throws Exception {
        return httpGet(url, null);
    }

    private static String httpPost(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("token", token);
        }
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        String resp = readBody(conn);
        conn.disconnect();
        return resp;
    }

    private static String readBody(HttpURLConnection conn) throws Exception {
        InputStream stream = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream; Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static String extract(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            return "";
        }
        start += marker.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return json.substring(start, end);
    }
}
