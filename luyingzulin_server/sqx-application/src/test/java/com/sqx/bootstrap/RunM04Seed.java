package com.sqx.bootstrap;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/** 一次性执行 M04 订单生命周期表与种子（联调验收用） */
public class RunM04Seed {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String base = "D:/ideaproject/luyingshebeizulin_work/luyingzulin_server/db/migration/";
        String schema = new String(Files.readAllBytes(Paths.get(base + "m04_order_lifecycle.sql")), "UTF-8");
        String seed = new String(Files.readAllBytes(Paths.get(base + "m04_order_lifecycle_seed.sql")), "UTF-8");
        try (Connection c = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/luyingzulin?useSSL=false&allowMultiQueries=true", "root", "hanstg")) {
            try (Statement s = c.createStatement()) {
                s.execute(schema);
                s.execute(seed);
            }
        }
        System.out.println("m04-seed-ok");
    }
}
