package com.sqx.bootstrap;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 下载露营商品真实图片，经 /file/upload 上传后回写 rent_goods / rent_goods_image。
 * 前置：后端已启动 http://127.0.0.1:9135/sqx_fast
 */
public class GoodsImageRefreshMain {
    static final String BASE = "http://127.0.0.1:9135/sqx_fast";
    static final String JDBC = "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    static final String DB_USER = "";
    static final String DB_PASS = "";
    static final String LOCAL_DOMAIN = "";

    static final Path IMAGE_DIR = Paths.get("D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/scripts/goods-images");
    static final Path SQL_OUT = Paths.get("D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration/m17_goods_images_real.sql");

    /** goodsId -> [coverUrl, detail1, detail2, detail3?] */
    static final Map<Integer, String[]> GOODS_SOURCES = new LinkedHashMap<>();

    static {
        GOODS_SOURCES.put(1, arr(
                "https://images.pexels.com/photos/599024/pexels-photo-599024.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/1687845/pexels-photo-1687845.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/1687843/pexels-photo-1687843.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/599024/pexels-photo-599024.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(2, arr(
                "https://images.pexels.com/photos/697244/pexels-photo-697244.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/697244/pexels-photo-697244.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/1687843/pexels-photo-1687843.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(3, arr(
                "https://images.pexels.com/photos/2693595/pexels-photo-2693595.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/2693595/pexels-photo-2693595.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/1687845/pexels-photo-1687845.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(4, arr(
                "https://images.pexels.com/photos/1687846/pexels-photo-1687846.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/1687846/pexels-photo-1687846.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/1687845/pexels-photo-1687845.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(5, arr(
                "https://images.pexels.com/photos/2777934/pexels-photo-2777934.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/2777934/pexels-photo-2777934.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/2777934/pexels-photo-2777934.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(6, arr(
                "https://images.pexels.com/photos/2901209/pexels-photo-2901209.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/2901209/pexels-photo-2901209.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/2901209/pexels-photo-2901209.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(7, arr(
                "https://images.pexels.com/photos/5126578/pexels-photo-5126578.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/5126578/pexels-photo-5126578.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/599024/pexels-photo-599024.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
        GOODS_SOURCES.put(8, arr(
                "https://images.pexels.com/photos/6991595/pexels-photo-6991595.jpeg?auto=compress&cs=tinysrgb&w=800",
                "https://images.pexels.com/photos/6991595/pexels-photo-6991595.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/2693595/pexels-photo-2693595.jpeg?auto=compress&cs=tinysrgb&w=1200"
        ));
    }

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Files.createDirectories(IMAGE_DIR);

        switchToLocalStorage();
        refreshConfigCache();

        Map<Integer, String> covers = new LinkedHashMap<>();
        Map<Integer, List<String>> details = new LinkedHashMap<>();

        for (Map.Entry<Integer, String[]> entry : GOODS_SOURCES.entrySet()) {
            int goodsId = entry.getKey();
            String[] urls = entry.getValue();
            String coverPath = uploadFromUrl(goodsId, "cover", urls[0]);
            covers.put(goodsId, coverPath);

            List<String> detailPaths = new ArrayList<>();
            for (int i = 1; i < urls.length; i++) {
                detailPaths.add(uploadFromUrl(goodsId, "detail" + i, urls[i]));
            }
            details.put(goodsId, detailPaths);
        }

        applyToDatabase(covers, details);
        writeMigrationSql(covers, details);
        System.out.println("goods-image-refresh-ok");
    }

    static void switchToLocalStorage() throws Exception {
        try (Connection c = DriverManager.getConnection(JDBC, DB_USER, DB_PASS)) {
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_storage_type'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_endpoint'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_bucket'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_accessKey'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_secretKey'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='file_cdn_domain'");
            exec(c, "UPDATE sys_config SET param_value='' WHERE param_key='sys_h5_domain'");
            exec(c, "DELETE FROM sys_config WHERE param_key='sms_mock_code'");
            exec(c, "UPDATE sys_config SET param_value='3' WHERE param_key='sms_provider'");
        }
        System.out.println("db config switched to ali oss");
    }

    static void refreshConfigCache() throws Exception {
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
        System.out.println("config refresh: " + shortText(refreshResp));
    }

    static String uploadFromUrl(int goodsId, String kind, String sourceUrl) throws Exception {
        String fileName = String.format("rg%03d-%s.jpg", goodsId, kind);
        Path localFile = IMAGE_DIR.resolve(fileName);
        download(sourceUrl, localFile);
        String uploadResp = uploadMultipart(BASE + "/file/upload", localFile, fileName);
        String path = extractNested(uploadResp, "data", "path");
        if (path.isEmpty()) {
            throw new IllegalStateException("upload failed for " + fileName + ": " + uploadResp);
        }
        System.out.println("uploaded " + fileName + " -> " + path);
        return path;
    }

    static void applyToDatabase(Map<Integer, String> covers, Map<Integer, List<String>> details) throws Exception {
        try (Connection c = DriverManager.getConnection(JDBC, DB_USER, DB_PASS)) {
            for (Map.Entry<Integer, String> e : covers.entrySet()) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE rent_goods SET cover_image=? WHERE id=?")) {
                    ps.setString(1, e.getValue());
                    ps.setInt(2, e.getKey());
                    ps.executeUpdate();
                }
            }
            for (Map.Entry<Integer, List<String>> e : details.entrySet()) {
                int goodsId = e.getKey();
                List<String> paths = e.getValue();
                for (int i = 0; i < paths.size(); i++) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE rent_goods_image SET image_url=? WHERE goods_id=? AND sort=?")) {
                        ps.setString(1, paths.get(i));
                        ps.setInt(2, goodsId);
                        ps.setInt(3, i + 1);
                        ps.executeUpdate();
                    }
                }
            }
        }
        System.out.println("database updated");
    }

    static void writeMigrationSql(Map<Integer, String> covers, Map<Integer, List<String>> details) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("-- =============================================================================\n");
        sb.append("-- 文件: m17_goods_images_real.sql\n");
        sb.append("-- 用途: 将商品封面/详情图从 picsum 占位替换为 /file/upload 上传后的真实地址\n");
        sb.append("-- =============================================================================\n\n");
        sb.append("SET NAMES utf8mb4;\n\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_storage_type';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_endpoint';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_bucket';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_Key';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_secret';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='file_cdn_domain';\n");
        sb.append("UPDATE sys_config SET param_value='' WHERE param_key='sys_h5_domain';\n\n");
        for (Map.Entry<Integer, String> e : covers.entrySet()) {
            sb.append("UPDATE rent_goods SET cover_image='").append(escape(e.getValue())).append("' WHERE id=")
                    .append(e.getKey()).append(";\n");
        }
        sb.append("\n");
        for (Map.Entry<Integer, List<String>> e : details.entrySet()) {
            int goodsId = e.getKey();
            List<String> paths = e.getValue();
            for (int i = 0; i < paths.size(); i++) {
                sb.append("UPDATE rent_goods_image SET image_url='").append(escape(paths.get(i)))
                        .append("' WHERE goods_id=").append(goodsId).append(" AND sort=").append(i + 1).append(";\n");
            }
        }
        Files.write(SQL_OUT, sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("wrote " + SQL_OUT);
    }

    static String escape(String s) {
        return s.replace("'", "''");
    }

    static void exec(Connection c, String sql) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    static void download(String url, Path target) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.connect();
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, target);
        }
        conn.disconnect();
    }

    static String uploadMultipart(String url, Path file, String fileName) throws Exception {
        String boundary = "----SqxBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        byte[] fileBytes = Files.readAllBytes(file);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n";
        body.write(header.getBytes(StandardCharsets.UTF_8));
        body.write(fileBytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        conn.getOutputStream().write(body.toByteArray());
        return read(conn);
    }

    static String loadCaptcha(String uuid) throws Exception {
        try (Connection c = DriverManager.getConnection(JDBC, DB_USER, DB_PASS)) {
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

    static String httpPost(String url, String body, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null) {
            conn.setRequestProperty("token", token);
        }
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
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

    static String[] arr(String... values) {
        return values;
    }
}
