package com.sqx.e2e;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

/**
 * E2E 支付模拟：本地无支付沙箱时，通过 JDBC 复现 handlePaySuccess / handleRenewPaySuccess 副作用（仅测试用）。
 */
public final class E2ePaySimulator {

    private E2ePaySimulator() {
    }

    /**
     * 将 pending_payment 订单模拟为支付成功（store 自提 → waiting_pickup）。
     */
    public static void simulateRentOrderPaySuccess(String orderNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }

        try (Connection connection = DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD)) {
            connection.setAutoCommit(false);
            try {
                OrderRow order = loadPendingOrder(connection, orderNo);
                if (order == null) {
                    throw new IllegalStateException("待支付订单不存在 orderNo=" + orderNo);
                }

                String targetStatus = "delivery".equals(order.pickupType) ? "delivering" : "waiting_pickup";
                String pickupCode = generatePickupCode();
                LocalDateTime now = LocalDateTime.now();

                try (PreparedStatement updateOrder = connection.prepareStatement(
                        "UPDATE rent_order SET status=?, pickup_code=?, pay_time=? "
                                + "WHERE id=? AND status='pending_payment'")) {
                    updateOrder.setString(1, targetStatus);
                    updateOrder.setString(2, pickupCode);
                    updateOrder.setTimestamp(3, Timestamp.valueOf(now));
                    updateOrder.setLong(4, order.id);
                    int updated = updateOrder.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("订单状态已变更，无法模拟支付 orderNo=" + orderNo);
                    }
                }

                try (PreparedStatement updateOccupy = connection.prepareStatement(
                        "UPDATE rent_stock_occupy SET status='OCCUPIED' "
                                + "WHERE order_id=? AND status='PRELOCK'")) {
                    updateOccupy.setLong(1, order.id);
                    updateOccupy.executeUpdate();
                }

                try (PreparedStatement insertPickup = connection.prepareStatement(
                        "INSERT INTO rent_pickup_code (order_id, order_no, pickup_code, expire_time, used) "
                                + "VALUES (?,?,?,?,0)")) {
                    insertPickup.setLong(1, order.id);
                    insertPickup.setString(2, orderNo);
                    insertPickup.setString(3, pickupCode);
                    insertPickup.setTimestamp(4, Timestamp.valueOf(order.endTime));
                    insertPickup.executeUpdate();
                }

                try (PreparedStatement insertPayment = connection.prepareStatement(
                        "INSERT INTO rent_order_payment (order_id, order_no, pay_no, pay_biz_type, amount, "
                                + "pay_channel, pay_status, pay_time) VALUES (?,?,?,?,?,?,?,?)")) {
                    insertPayment.setLong(1, order.id);
                    insertPayment.setString(2, orderNo);
                    insertPayment.setString(3, "E2E-MOCK-" + System.currentTimeMillis());
                    insertPayment.setString(4, "rent_order");
                    insertPayment.setBigDecimal(5, order.payAmount);
                    insertPayment.setString(6, "e2e");
                    insertPayment.setString(7, "success");
                    insertPayment.setTimestamp(8, Timestamp.valueOf(now));
                    insertPayment.executeUpdate();
                }

                try (PreparedStatement insertLog = connection.prepareStatement(
                        "INSERT INTO rent_order_log (order_id, order_no, from_status, to_status, "
                                + "operator_type, remark) VALUES (?,?,?,?,?,?)")) {
                    insertLog.setLong(1, order.id);
                    insertLog.setString(2, orderNo);
                    insertLog.setString(3, "pending_payment");
                    insertLog.setString(4, targetStatus);
                    insertLog.setString(5, "system");
                    insertLog.setString(6, "E2E 模拟支付成功");
                    insertLog.executeUpdate();
                }

                simulateDepositFreezeForOrder(connection, orderNo);
                syncMerchantOrderOnPay(connection, order.id);

                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new IllegalStateException("模拟支付失败 orderNo=" + orderNo, ex);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("模拟支付 JDBC 失败 orderNo=" + orderNo, ex);
        }
    }

    /**
     * 将 pending 续租单模拟为支付成功（延长租期、写入 rent_renew 支付流水）。
     */
    public static void simulateRenewPaySuccess(String renewNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }

        try (Connection connection = DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD)) {
            connection.setAutoCommit(false);
            try {
                RenewRow renew = loadPendingRenew(connection, renewNo);
                if (renew == null) {
                    throw new IllegalStateException("待支付续租单不存在 renewNo=" + renewNo);
                }
                if ("success".equals(renew.payStatus)) {
                    connection.commit();
                    return;
                }

                OrderRow order = loadOrderById(connection, renew.orderId);
                if (order == null) {
                    throw new IllegalStateException("续租关联订单不存在 orderId=" + renew.orderId);
                }

                int days = resolveDays(order.startTime, renew.newEndTime);
                LocalDateTime now = LocalDateTime.now();
                java.math.BigDecimal newRentAmount = order.rentAmount.add(renew.renewAmount);

                try (PreparedStatement updateOccupy = connection.prepareStatement(
                        "UPDATE rent_stock_occupy SET end_time=? "
                                + "WHERE order_id=? AND status='OCCUPIED'")) {
                    updateOccupy.setTimestamp(1, Timestamp.valueOf(renew.newEndTime));
                    updateOccupy.setLong(2, renew.orderId);
                    updateOccupy.executeUpdate();
                }

                try (PreparedStatement updateOrder = connection.prepareStatement(
                        "UPDATE rent_order SET end_time=?, days=?, rent_amount=? "
                                + "WHERE id=? AND status='renting'")) {
                    updateOrder.setTimestamp(1, Timestamp.valueOf(renew.newEndTime));
                    updateOrder.setInt(2, days);
                    updateOrder.setBigDecimal(3, newRentAmount);
                    updateOrder.setLong(4, renew.orderId);
                    int updated = updateOrder.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("订单非 renting，无法模拟续租支付 orderId=" + renew.orderId);
                    }
                }

                try (PreparedStatement updateItem = connection.prepareStatement(
                        "UPDATE rent_order_item SET end_time=?, days=? WHERE order_id=?")) {
                    updateItem.setTimestamp(1, Timestamp.valueOf(renew.newEndTime));
                    updateItem.setInt(2, days);
                    updateItem.setLong(3, renew.orderId);
                    updateItem.executeUpdate();
                }

                try (PreparedStatement updateRenew = connection.prepareStatement(
                        "UPDATE rent_renew_record SET pay_status='success', pay_time=? "
                                + "WHERE id=? AND pay_status='pending'")) {
                    updateRenew.setTimestamp(1, Timestamp.valueOf(now));
                    updateRenew.setLong(2, renew.id);
                    int updated = updateRenew.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("续租单状态已变更 renewNo=" + renewNo);
                    }
                }

                try (PreparedStatement insertPayment = connection.prepareStatement(
                        "INSERT INTO rent_order_payment (order_id, order_no, pay_no, pay_biz_type, amount, "
                                + "pay_channel, pay_status, pay_time) VALUES (?,?,?,?,?,?,?,?)")) {
                    insertPayment.setLong(1, renew.orderId);
                    insertPayment.setString(2, renew.orderNo);
                    insertPayment.setString(3, "E2E-RENEW-" + System.currentTimeMillis());
                    insertPayment.setString(4, "rent_renew");
                    insertPayment.setBigDecimal(5, renew.renewAmount);
                    insertPayment.setString(6, "e2e");
                    insertPayment.setString(7, "success");
                    insertPayment.setTimestamp(8, Timestamp.valueOf(now));
                    insertPayment.executeUpdate();
                }

                try (PreparedStatement insertLog = connection.prepareStatement(
                        "INSERT INTO rent_order_log (order_id, order_no, from_status, to_status, "
                                + "operator_type, remark) VALUES (?,?,?,?,?,?)")) {
                    insertLog.setLong(1, renew.orderId);
                    insertLog.setString(2, renew.orderNo);
                    insertLog.setString(3, "renting");
                    insertLog.setString(4, "renting");
                    insertLog.setString(5, "system");
                    insertLog.setString(6, "E2E 模拟续租支付成功，延长 " + renew.extraDays + " 天");
                    insertLog.executeUpdate();
                }

                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new IllegalStateException("模拟续租支付失败 renewNo=" + renewNo, ex);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("模拟续租支付 JDBC 失败 renewNo=" + renewNo, ex);
        }
    }

    /**
     * 将押金待支付单模拟为支付成功。
     */
    public static void simulateDepositPaySuccess(String payNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }

        try (Connection connection = DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD)) {
            connection.setAutoCommit(false);
            try {
                Long userId = null;
                java.math.BigDecimal payAmount = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT user_id, amount, status FROM user_deposit_log "
                                + "WHERE order_no=? AND log_type='pay' LIMIT 1")) {
                    ps.setString(1, payNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("押金支付单不存在 payNo=" + payNo);
                        }
                        if ("completed".equals(rs.getString("status"))) {
                            connection.commit();
                            return;
                        }
                        userId = rs.getLong("user_id");
                        payAmount = rs.getBigDecimal("amount");
                    }
                }

                try (PreparedStatement updateLog = connection.prepareStatement(
                        "UPDATE user_deposit_log SET status='completed', remark='押金缴纳成功' "
                                + "WHERE order_no=? AND status='pending'")) {
                    updateLog.setString(1, payNo);
                    int updated = updateLog.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("押金支付单状态已变更 payNo=" + payNo);
                    }
                }

                if (userId != null && payAmount != null) {
                    try (PreparedStatement ensureDeposit = connection.prepareStatement(
                            "INSERT INTO user_deposit (user_id, total_amount, frozen_amount, status) "
                                    + "SELECT ?, 0, 0, 1 FROM DUAL WHERE NOT EXISTS ("
                                    + "SELECT 1 FROM user_deposit WHERE user_id=?)")) {
                        ensureDeposit.setLong(1, userId);
                        ensureDeposit.setLong(2, userId);
                        ensureDeposit.executeUpdate();
                    }
                    try (PreparedStatement updateDeposit = connection.prepareStatement(
                            "UPDATE user_deposit SET total_amount = total_amount + ?, update_time = NOW() "
                                    + "WHERE user_id = ?")) {
                        updateDeposit.setBigDecimal(1, payAmount);
                        updateDeposit.setLong(2, userId);
                        updateDeposit.executeUpdate();
                    }
                }

                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new IllegalStateException("模拟押金支付失败 payNo=" + payNo, ex);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("模拟押金支付 JDBC 失败 payNo=" + payNo, ex);
        }
    }

    /**
     * 将会员待支付订单模拟为支付成功（开通 VIP）。
     */
    public static void simulateVipPaySuccess(String orderNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("MySQL 驱动不可用", ex);
        }

        try (Connection connection = DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD)) {
            connection.setAutoCommit(false);
            try {
                Long userId = null;
                try (PreparedStatement ps = connection.prepareStatement(
                        "SELECT user_id, status FROM vip_order WHERE order_no=? LIMIT 1")) {
                    ps.setString(1, orderNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("会员订单不存在 orderNo=" + orderNo);
                        }
                        if ("paid".equals(rs.getString("status"))) {
                            connection.commit();
                            return;
                        }
                        userId = rs.getLong("user_id");
                    }
                }

                LocalDateTime now = LocalDateTime.now();
                try (PreparedStatement updateOrder = connection.prepareStatement(
                        "UPDATE vip_order SET status='paid', pay_channel='e2e', pay_trade_no=?, pay_time=? "
                                + "WHERE order_no=? AND status='pending_payment'")) {
                    updateOrder.setString(1, "E2E-VIP-" + System.currentTimeMillis());
                    updateOrder.setTimestamp(2, Timestamp.valueOf(now));
                    updateOrder.setString(3, orderNo);
                    int updated = updateOrder.executeUpdate();
                    if (updated == 0) {
                        throw new IllegalStateException("会员订单状态已变更 orderNo=" + orderNo);
                    }
                }

                if (userId != null) {
                    try (PreparedStatement updateUser = connection.prepareStatement(
                            "UPDATE tb_user SET is_vip=1, user_type=2 WHERE user_id=?")) {
                        updateUser.setLong(1, userId);
                        updateUser.executeUpdate();
                    }
                }

                connection.commit();
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new IllegalStateException("模拟会员支付失败 orderNo=" + orderNo, ex);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("模拟会员支付 JDBC 失败 orderNo=" + orderNo, ex);
        }
    }

    static String queryRenewPayStatus(String renewNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT pay_status FROM rent_renew_record WHERE renew_no=? LIMIT 1")) {
                ps.setString(1, renewNo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询续租单状态失败 renewNo=" + renewNo, ex);
        }
    }

    static String queryOrderEndTime(String orderNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT DATE_FORMAT(end_time,'%Y-%m-%d %H:%i:%s') FROM rent_order "
                                 + "WHERE order_no=? LIMIT 1")) {
                ps.setString(1, orderNo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询订单结束时间失败 orderNo=" + orderNo, ex);
        }
    }

    static java.math.BigDecimal queryInspectionRefundAmount(String orderNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT refund_amount FROM rent_inspection WHERE order_no=? "
                                 + "ORDER BY id DESC LIMIT 1")) {
                ps.setString(1, orderNo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getBigDecimal(1) : null;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询验收退押失败 orderNo=" + orderNo, ex);
        }
    }

    static int countPaymentsByBizType(String orderNo, String payBizType) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT COUNT(*) FROM rent_order_payment p "
                                 + "JOIN rent_order o ON p.order_id=o.id "
                                 + "WHERE o.order_no=? AND p.pay_biz_type=? AND p.pay_status='success'")) {
                ps.setString(1, orderNo);
                ps.setString(2, payBizType);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询支付流水失败 orderNo=" + orderNo, ex);
        }
    }

    static String queryOrderStatus(String orderNo) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT status FROM rent_order WHERE order_no=? LIMIT 1")) {
                ps.setString(1, orderNo);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询订单状态失败 orderNo=" + orderNo, ex);
        }
    }

    static String queryStockOccupyStatus(Long orderId) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    E2eServerSupport.MYSQL_JDBC_URL,
                    E2eServerSupport.MYSQL_USER,
                    E2eServerSupport.MYSQL_PASSWORD);
                 PreparedStatement ps = connection.prepareStatement(
                         "SELECT status FROM rent_stock_occupy WHERE order_id=? "
                                 + "ORDER BY id DESC LIMIT 1")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("查询库存预占失败 orderId=" + orderId, ex);
        }
    }

    /**
     * 对齐 handlePaySuccess → userDepositService.freezeForOrder 副作用（M06 押金流水联调）。
     */
    private static void simulateDepositFreezeForOrder(Connection connection, String orderNo) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT user_id, deposit_amount FROM rent_order WHERE order_no=? LIMIT 1")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                long userId = rs.getLong("user_id");
                java.math.BigDecimal depositAmount = rs.getBigDecimal("deposit_amount");
                if (depositAmount == null || depositAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    return;
                }
                try (PreparedStatement updateDeposit = connection.prepareStatement(
                        "UPDATE user_deposit SET total_amount = total_amount + ?, "
                                + "frozen_amount = frozen_amount + ?, update_time = NOW() "
                                + "WHERE user_id = ?")) {
                    updateDeposit.setBigDecimal(1, depositAmount);
                    updateDeposit.setBigDecimal(2, depositAmount);
                    updateDeposit.setLong(3, userId);
                    updateDeposit.executeUpdate();
                }
                try (PreparedStatement insertLog = connection.prepareStatement(
                        "INSERT INTO user_deposit_log (log_no, user_id, log_type, amount, order_no, status, remark) "
                                + "VALUES (?,?,?,?,?,?,?)")) {
                    insertLog.setString(1, "E2E-DL-" + System.currentTimeMillis());
                    insertLog.setLong(2, userId);
                    insertLog.setString(3, "freeze");
                    insertLog.setBigDecimal(4, depositAmount);
                    insertLog.setString(5, orderNo);
                    insertLog.setString(6, "completed");
                    insertLog.setString(7, "E2E 订单押金冻结");
                    insertLog.executeUpdate();
                }
            }
        }
    }

    /**
     * 支付成功后同步 merchant_order（LINK-{rentOrderId}），对齐 MerchantOrderSyncSupport.syncOnPaySuccess。
     */
    private static void syncMerchantOrderOnPay(Connection connection, long orderId) throws Exception {
        long merchantId = 0;
        String pickupType;
        String contactName;
        String contactPhone;
        java.math.BigDecimal payAmount;
        long userId;
        String orderNo;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT merchant_id, pickup_type, contact_name, contact_phone, pay_amount, user_id, order_no "
                        + "FROM rent_order WHERE id=?")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                merchantId = rs.getLong("merchant_id");
                if (merchantId <= 0) {
                    return;
                }
                pickupType = rs.getString("pickup_type");
                contactName = rs.getString("contact_name");
                contactPhone = rs.getString("contact_phone");
                payAmount = rs.getBigDecimal("pay_amount");
                userId = rs.getLong("user_id");
                orderNo = rs.getString("order_no");
            }
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO merchant_order (merchant_order_no, merchant_id, rent_order_id, rent_order_no, "
                        + "user_id, goods_name, status, contact_name, contact_phone, pickup_type, pay_amount) "
                        + "SELECT ?, ?, ?, ?, ?, "
                        + "(SELECT goods_name FROM rent_order_item WHERE order_id=? LIMIT 1), "
                        + "'pending_prepare', ?, ?, ?, ? "
                        + "FROM DUAL WHERE NOT EXISTS ("
                        + "SELECT 1 FROM merchant_order WHERE rent_order_id=? AND deleted=0)")) {
            ps.setString(1, "LINK-" + orderId);
            ps.setLong(2, merchantId);
            ps.setLong(3, orderId);
            ps.setString(4, orderNo);
            ps.setLong(5, userId);
            ps.setLong(6, orderId);
            ps.setString(7, contactName);
            ps.setString(8, contactPhone);
            ps.setString(9, pickupType);
            ps.setBigDecimal(10, payAmount);
            ps.setLong(11, orderId);
            ps.executeUpdate();
        }
    }

    private static OrderRow loadPendingOrder(Connection connection, String orderNo) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, pickup_type, pay_amount, end_time FROM rent_order "
                        + "WHERE order_no=? AND status='pending_payment' LIMIT 1")) {
            ps.setString(1, orderNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                OrderRow row = new OrderRow();
                row.id = rs.getLong("id");
                row.pickupType = rs.getString("pickup_type");
                row.payAmount = rs.getBigDecimal("pay_amount");
                Timestamp endTime = rs.getTimestamp("end_time");
                row.endTime = endTime == null ? LocalDateTime.now().plusDays(2) : endTime.toLocalDateTime();
                return row;
            }
        }
    }

    private static OrderRow loadOrderById(Connection connection, Long orderId) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, start_time, rent_amount FROM rent_order WHERE id=? LIMIT 1")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                OrderRow row = new OrderRow();
                row.id = rs.getLong("id");
                Timestamp startTime = rs.getTimestamp("start_time");
                row.startTime = startTime == null ? LocalDateTime.now() : startTime.toLocalDateTime();
                row.rentAmount = rs.getBigDecimal("rent_amount");
                if (row.rentAmount == null) {
                    row.rentAmount = java.math.BigDecimal.ZERO;
                }
                return row;
            }
        }
    }

    private static RenewRow loadPendingRenew(Connection connection, String renewNo) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, order_id, order_no, extra_days, new_end_time, renew_amount, pay_status "
                        + "FROM rent_renew_record WHERE renew_no=? LIMIT 1")) {
            ps.setString(1, renewNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                RenewRow row = new RenewRow();
                row.id = rs.getLong("id");
                row.orderId = rs.getLong("order_id");
                row.orderNo = rs.getString("order_no");
                row.extraDays = rs.getInt("extra_days");
                Timestamp newEnd = rs.getTimestamp("new_end_time");
                row.newEndTime = newEnd == null ? LocalDateTime.now() : newEnd.toLocalDateTime();
                row.renewAmount = rs.getBigDecimal("renew_amount");
                if (row.renewAmount == null) {
                    row.renewAmount = java.math.BigDecimal.ZERO;
                }
                row.payStatus = rs.getString("pay_status");
                return row;
            }
        }
    }

    private static int resolveDays(LocalDateTime start, LocalDateTime end) {
        long between = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
        return (int) Math.max(between, 1);
    }

    private static String generatePickupCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 999999);
        return String.valueOf(code);
    }

    private static final class OrderRow {
        private Long id;
        private String pickupType;
        private java.math.BigDecimal payAmount;
        private LocalDateTime endTime;
        private LocalDateTime startTime;
        private java.math.BigDecimal rentAmount;
    }

    private static final class RenewRow {
        private Long id;
        private Long orderId;
        private String orderNo;
        private int extraDays;
        private LocalDateTime newEndTime;
        private java.math.BigDecimal renewAmount;
        private String payStatus;
    }
}
