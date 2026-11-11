package com.sqx.bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** 幂等执行 M29（支付配置占位项） */
public class RunM29PayConfig {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Path file = Paths.get("D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration/m29_pay_config_template.sql");
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true",
                "root",
                "hanstg")) {
            String sql = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            System.out.println("run " + file.getFileName());
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            System.out.println("done " + file.getFileName());
            printPayConfigs(connection);
        }
        System.out.println("m29-pay-config-ok");
    }

    private static void printPayConfigs(Connection connection) throws Exception {
        String query = "SELECT param_key, IF(param_value IS NULL OR param_value = '', '(待填写)', '(已填写)') AS fill_status "
                + "FROM sys_config WHERE param_type = 'pay' OR param_key LIKE 'pay_%' OR param_key IN "
                + "('file_public_base_url', 'sys_h5_domain', 'sys_wx_key_path', 'sys_wechat_app_appid', "
                + "'sys_wechat_h5_appid', 'sys_wechat_jsapi_appid', 'sys_wechat_jsapi_secret', 'sys_user_mp_secret') "
                + "ORDER BY param_key";
        System.out.println("---- pay sys_config ----");
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                System.out.println(rs.getString("param_key") + " " + rs.getString("fill_status"));
            }
        }
    }
}
