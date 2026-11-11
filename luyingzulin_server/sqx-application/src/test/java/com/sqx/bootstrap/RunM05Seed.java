package com.sqx.bootstrap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 一次性执行 M05 用户账户表与种子（联调验收用） */
public class RunM05Seed {
    public static void main(String[] args) throws Exception {
        String base = "D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration/";
        String schema = new String(Files.readAllBytes(Paths.get(base + "m05_user_account.sql")), "UTF-8");
        String seed = new String(Files.readAllBytes(Paths.get(base + "m05_user_account_seed.sql")), "UTF-8");
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false&allowMultiQueries=true", "root", "hanstg")) {
            try (Statement s = c.createStatement()) {
                s.execute(schema);
                s.execute(seed);
            }
        }
        System.out.println("m05-seed-ok");
    }
}
