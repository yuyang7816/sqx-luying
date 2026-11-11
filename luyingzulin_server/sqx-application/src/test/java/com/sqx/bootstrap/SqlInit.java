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

/**
 * 本地 dev 库幂等初始化：基础表结构 + 露营租赁 migration 链（不再导入手办商城 03_init_config）。
 */
public class SqlInit {
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/luyingzulin?useUnicode=true&characterEncoding=utf-8&useSSL=false&allowMultiQueries=true";
        Path projectRoot = Paths.get("D:/ideaproject/luyingshebeizulin_work");

        List<Path> files = Arrays.asList(
                Paths.get("D:/ideaproject/shouban_guaji/shoubanshangcheng_all/java_server/sql/01_schema_base.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m00_sys_config_baseline.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/rental_v1.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m00_integration_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m01_tabbar_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m02_goods_browse_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m03_cart_order_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m04_order_lifecycle.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m04_order_lifecycle_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m05_user_account.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m05_user_account_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m06_user_assets.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m06_user_assets_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m07_merchant.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m07_merchant_seed.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m08_vip_purchase.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m09_cleanup_shouban_legacy.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m22_sys_oa_oauth_enabled.sql"),
                projectRoot.resolve("luyingzulin_server/db/migration/m23_merchant_apply_store_extend.sql")
        );

        try (Connection connection = DriverManager.getConnection(url, "root", "hanstg")) {
            for (Path file : files) {
                if (!Files.exists(file)) {
                    throw new IllegalStateException("missing sql file: " + file);
                }
                String sql = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                System.out.println("run " + file);
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
                System.out.println("done " + file.getFileName());
            }
        }
        System.out.println("sql-init-ok");
    }
}
