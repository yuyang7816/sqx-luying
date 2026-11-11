package com.sqx.bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/** 幂等执行 M22（公众号授权开关）与 M23（入驻申请门店扩展字段） */
public class RunM22M23 {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        Path base = Paths.get("D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration");
        List<Path> files = Arrays.asList(
                base.resolve("m22_sys_oa_oauth_enabled.sql"),
                base.resolve("m23_merchant_apply_store_extend.sql")
        );
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true",
                "root",
                "hanstg")) {
            for (Path file : files) {
                String sql = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                System.out.println("run " + file.getFileName());
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
                System.out.println("done " + file.getFileName());
            }
        }
        System.out.println("m22-m23-ok");
    }
}
