package com.sqx.bootstrap;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** 执行查询并输出首行首列（供脚本调用） */
public class SqlQuery {
    static final String JDBC = "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("missing sql");
        }
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection c = DriverManager.getConnection(JDBC, "root", "hanstg");
             PreparedStatement ps = c.prepareStatement(args[0])) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.print(rs.getString(1));
                }
            }
        }
    }
}
