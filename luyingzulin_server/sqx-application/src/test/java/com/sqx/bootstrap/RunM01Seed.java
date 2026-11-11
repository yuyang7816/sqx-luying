package com.sqx.bootstrap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 一次性执行 M01 TabBar 种子（联调验收用） */
public class RunM01Seed {
    public static void main(String[] args) throws Exception {
        String sql = new String(Files.readAllBytes(Paths.get(
                "D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration/m01_tabbar_seed.sql")), "UTF-8");
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false&allowMultiQueries=true", "root", "hanstg")) {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            }
        }
        System.out.println("m01-seed-ok");
    }
}
