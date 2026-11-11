package com.sqx.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 二级分销 E2E：A→B→C 邀请链，C 下单验收完成后 B 得一级、A 得二级佣金。
 */
@Tag("e2e")
class ReferralLevelCommissionE2ETest {

    private static final String INVITE_A = "E2E-INV-A";
    private static final String INVITE_B = "E2E-INV-B";
    private static final String INVITE_C = "E2E-INV-C";
    private static final String PHONE_B = "13900006601";
    private static final String PHONE_C = "13900006602";
    private static final String PASSWORD = "123456";
    private static final String RENT_START = "2027-03-10 10:00:00";
    private static final String RENT_END = "2027-03-12 18:00:00";

    @BeforeAll
    static void assumeServerReady() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：购物车/订单接口未部署");
        assumeTrue(E2eServerSupport.isM04OrderLifecycleApiAvailable(),
                () -> "E2E 跳过：归还/验收接口未部署");
    }

    @Test
    void level1AndLevel2CommissionAfterOrderCompleted() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        Long userAId = findUserIdByPhone(E2eServerSupport.TEST_PHONE);
        assumeTrue(userAId != null, () -> "E2E 跳过：测试用户不存在");

        ensureInvitationCode(userAId, INVITE_A);
        Long userBId = ensureUserWithSuperior(PHONE_B, "e2e-inv-b", INVITE_A, INVITE_B);
        ensureUserWithSuperior(PHONE_C, "e2e-inv-c", INVITE_B, INVITE_C);

        String tokenC = appLogin(PHONE_C);
        Map<String, Object> createResult = createStorePickupOrder(tokenC);
        String orderNo = (String) createResult.get("orderNo");
        long orderId = ((Number) createResult.get("orderId")).longValue();

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, tokenC)
                .when()
                .post("/app/rental/pickup/confirm/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, tokenC)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"returnMethod\":\"store\","
                        + "\"returnDate\":\"2027-03-12\",\"remark\":\"E2E-REF归还\"}")
                .when()
                .post("/app/rental/return/apply")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, tokenC)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"deviceCondition\":\"good\","
                        + "\"deductAmount\":0,\"remark\":\"E2E-REF验收\"}")
                .when()
                .post("/app/merchant/inspection/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("completed", E2ePaySimulator.queryOrderStatus(orderNo));

        String adminToken = E2eServerSupport.adminLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"userId\":" + userBId + "}")
                .when()
                .post("/admin/referral/commission/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.amount", greaterThan(0.0f));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"userId\":" + userAId + "}")
                .when()
                .post("/admin/referral/commission/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.amount", greaterThan(0.0f));
    }

    private static Long findUserIdByPhone(String phone) {
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT user_id FROM tb_user WHERE phone=? LIMIT 1")) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static void ensureInvitationCode(Long userId, String code) {
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE tb_user SET invitation_code=? WHERE user_id=?")) {
            ps.setString(1, code);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("设置邀请码失败", e);
        }
    }

    private static Long ensureUserWithSuperior(String phone, String userName, String superiorCode, String inviteCode) {
        Long existing = findUserIdByPhone(phone);
        if (existing != null) {
            try (Connection c = openConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE tb_user SET superior_invitation_code=?, invitation_code=?, password=? WHERE user_id=?")) {
                ps.setString(1, superiorCode);
                ps.setString(2, inviteCode);
                ps.setString(3, "e10adc3949ba59abbe56e057f20f883e");
                ps.setLong(4, existing);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new IllegalStateException("更新邀请关系失败", e);
            }
            return existing;
        }
        try (Connection c = openConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO tb_user (user_name, phone, password, status, platform, user_type, invitation_code, superior_invitation_code) "
                             + "VALUES (?,?,?,1,'h5',1,?,?)")) {
            ps.setString(1, userName);
            ps.setString(2, phone);
            ps.setString(3, "e10adc3949ba59abbe56e057f20f883e");
            ps.setString(4, inviteCode);
            ps.setString(5, superiorCode);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("创建测试用户失败", e);
        }
        Long created = findUserIdByPhone(phone);
        assertNotNull(created, "创建用户后应能查到 userId");
        return created;
    }

    private static String appLogin(String phone) {
        return given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + phone + "\",\"password\":\"" + PASSWORD + "\"}")
                .when()
                .post("/app/base/auth/phonePasswordLogin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    private static Map<String, Object> createStorePickupOrder(String token) {
        Number cartId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\"" + RENT_START
                        + "\",\"endTime\":\"" + RENT_END + "\",\"qty\":1}")
                .when()
                .post("/app/rental/cart/add")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");

        return given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\"" + RENT_START
                        + "\",\"endTime\":\"" + RENT_END + "\",\"quantity\":1}],"
                        + "\"pickupType\":\"store\",\"contactName\":\"E2E-REF\",\"contactPhone\":\""
                        + PHONE_C + "\",\"cartIds\":[" + cartId.longValue() + "]}")
                .when()
                .post("/app/rental/order/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.orderNo", notNullValue())
                .extract()
                .path("data");
    }

    private static Connection openConnection() throws Exception {
        return DriverManager.getConnection(
                E2eServerSupport.MYSQL_JDBC_URL,
                E2eServerSupport.MYSQL_USER,
                E2eServerSupport.MYSQL_PASSWORD);
    }
}
