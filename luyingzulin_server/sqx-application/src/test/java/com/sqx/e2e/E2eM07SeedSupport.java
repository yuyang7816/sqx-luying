package com.sqx.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * M07 E2E/联调：当本地库未导入 m07_merchant_seed.sql 时，幂等补齐演示商户数据。
 */
public final class E2eM07SeedSupport {

    private E2eM07SeedSupport() {
    }

    public static void ensureMerchantSeedIfMissing() {
        E2eSqlScriptSupport.ensureTableExists("merchant", "db/migration/m07_merchant.sql");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }
        try (Connection connection = DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD)) {
            if (countByPhone(connection, E2eServerSupport.MERCHANT_PHONE) > 0) {
                return;
            }
            connection.setAutoCommit(false);
            try {
                exec(connection,
                        "INSERT INTO tb_user (user_name, phone, password, status, platform, user_type, invitation_code) "
                                + "SELECT 'merchant001', ?, 'e10adc3949ba59abbe56e057f20f883e', 1, 'h5', 1, 'MCH001' "
                                + "FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM tb_user WHERE phone=?)",
                        E2eServerSupport.MERCHANT_PHONE, E2eServerSupport.MERCHANT_PHONE);
                exec(connection,
                        "INSERT INTO merchant (merchant_no, user_id, name, contact_name, contact_phone, status) "
                                + "SELECT 'MCH-SEED-001', u.user_id, '山野营地户外租赁', '李晨', ?, 'active' "
                                + "FROM tb_user u WHERE u.phone=? "
                                + "AND NOT EXISTS (SELECT 1 FROM merchant m WHERE m.merchant_no='MCH-SEED-001')",
                        E2eServerSupport.MERCHANT_PHONE, E2eServerSupport.MERCHANT_PHONE);
                exec(connection,
                        "INSERT INTO merchant_store (store_no, merchant_id, name, address, province, city, district, "
                                + "contact_phone, business_hours, service_area, status) "
                                + "SELECT 'STORE-SEED-001', m.id, '西安高新营地店', "
                                + "'陕西省西安市高新区锦业路 32 号营地仓配中心', '陕西省', '西安市', '高新区', "
                                + "'02981236688', '09:00-21:00', '高新区、雁塔区、曲江新区', 1 "
                                + "FROM merchant m WHERE m.merchant_no='MCH-SEED-001' "
                                + "AND NOT EXISTS (SELECT 1 FROM merchant_store s WHERE s.store_no='STORE-SEED-001')");
                exec(connection,
                        "INSERT INTO merchant_staff (merchant_id, store_id, name, role, phone, status) "
                                + "SELECT m.id, s.id, '赵店长', '门店核销', '13888888856', 1 "
                                + "FROM merchant m JOIN merchant_store s ON s.merchant_id=m.id AND s.store_no='STORE-SEED-001' "
                                + "WHERE m.merchant_no='MCH-SEED-001' "
                                + "AND NOT EXISTS (SELECT 1 FROM merchant_staff st WHERE st.merchant_id=m.id "
                                + "AND st.phone='13888888856' AND st.deleted=0)");
                exec(connection,
                        "UPDATE rent_goods g JOIN merchant m ON m.merchant_no='MCH-SEED-001' "
                                + "SET g.merchant_id=m.id WHERE g.id IN (1,2,3,4,5) "
                                + "AND (g.merchant_id IS NULL OR g.merchant_id=0)");
                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
            if (countByPhone(connection, E2eServerSupport.MERCHANT_PHONE) <= 0) {
                throw new IllegalStateException("M07 商户账号仍未写入 tb_user，请执行 m07_merchant_seed.sql");
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("补齐 M07 商户种子失败，请手动执行 m07_merchant_seed.sql", ex);
        }
    }

    private static int countByPhone(Connection connection, String phone) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM tb_user WHERE phone=?")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private static void exec(Connection connection, String sql, String... params) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }
}
