package com.sqx.bootstrap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 执行单条 SQL（供脚本调用） */
public class SqlExec {
    static final String JDBC = "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("missing sql");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(JDBC, "root", "hanstg");
             Statement st = c.createStatement()) {
            st.executeUpdate(args[0]);
        }
        System.out.println("ok");
    }
}
