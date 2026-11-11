package com.sqx.bootstrap;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.UUID;

public class SmokeTest {
    static final String BASE = "http://127.0.0.1:9135/sqx_fast";

    public static void main(String[] args) throws Exception {
        String uuid = UUID.randomUUID().toString();
        httpGet(BASE + "/captcha.jpg?uuid=" + uuid);
        String captcha = loadCaptcha(uuid);
        System.out.println("captcha=" + captcha);

        String adminLoginBody = "{\"username\":\"admin\",\"password\":\"123456\",\"uuid\":\"" + uuid + "\",\"captcha\":\"" + captcha + "\"}";
        String adminLogin = httpPost(BASE + "/sys/login", adminLoginBody, null);
        System.out.println("adminLogin=" + adminLogin);
        String adminToken = extract(adminLogin, "token");
        if (adminToken.isEmpty()) {
            throw new IllegalStateException("admin login failed");
        }

        String banner = httpPost(BASE + "/admin/base/banner/page", "{\"page\":1,\"limit\":10}", adminToken);
        System.out.println("bannerPage=" + shortText(banner));

        String users = httpPost(BASE + "/admin/base/user/list", "{\"page\":1,\"limit\":10}", adminToken);
        System.out.println("userList=" + shortText(users));

        String appLoginBody = "{\"phoneNo\":\"13812348888\",\"password\":\"123456\"}";
        String appLogin = httpPost(BASE + "/app/base/auth/phonePasswordLogin", appLoginBody, null);
        System.out.println("appLogin=" + shortText(appLogin));
        String appToken = extractNested(appLogin, "data", "token");
        if (appToken.isEmpty()) {
            throw new IllegalStateException("app login failed");
        }

        String appInfo = httpGet(BASE + "/app/base/user/info", appToken);
        System.out.println("appUserInfo=" + shortText(appInfo));

        int docStatus = httpStatus(BASE + "/doc.html");
        System.out.println("docStatus=" + docStatus);
        System.out.println("m00-integration-ok");
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
        conn.getInputStream().close();
        conn.disconnect();
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
        return text.length() > 180 ? text.substring(0, 180) + "..." : text;
    }
}
