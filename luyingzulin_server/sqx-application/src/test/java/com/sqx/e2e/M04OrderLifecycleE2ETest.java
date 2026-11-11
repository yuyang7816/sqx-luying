package com.sqx.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * M04 E2E：支付成功→取货→租赁中→续租→归还→验收完成。
 * <p>
 * 对齐《后端接口开发改造计划》1.5 用例 3 与 §3 续租/归还/验收全链路。
 * 依赖 server:9135 与 MySQL 种子链（含 m04_order_lifecycle.sql）。
 */
@Tag("e2e")
class M04OrderLifecycleE2ETest {

    @BeforeAll
    static void assumeServerAndM04ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：M03 购物车/订单接口未部署");
        assumeTrue(E2eServerSupport.isM04OrderLifecycleApiAvailable(),
                () -> "E2E 跳过：M04 取货/续租/归还接口未部署，请重启 server");
    }

    @Test
    void orderLifecycleFullChainPickupRenewReturnInspection() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法模拟支付");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        String orderNo = (String) createResult.get("orderNo");
        long orderId = ((Number) createResult.get("orderId")).longValue();

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        assertEquals("waiting_pickup", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/pickup/code/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.pickupCode", notNullValue())
                .body("data.orderNo", equalTo(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .post("/app/rental/pickup/confirm/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("renting", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/renew/preview/" + orderId
                        + "?newEndTime=" + E2eServerSupport.M04_RENT_END_AFTER_RENEW.replace(" ", "%20"))
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.extraHours", equalTo(24))
                .body("data.newEndTime", notNullValue())
                .body("data.renewAmount", notNullValue())
                .body("data.hourUnitPrice", notNullValue());

        Map<String, Object> renewCreate = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"newEndTime\":\"" + E2eServerSupport.M04_RENT_END_AFTER_RENEW + "\"}")
                .when()
                .post("/app/rental/renew/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.renewNo", notNullValue())
                .body("data.payStatus", equalTo("pending"))
                .extract()
                .path("data");

        String renewNo = (String) renewCreate.get("renewNo");
        E2ePaySimulator.simulateRenewPaySuccess(renewNo);

        assertEquals("success", E2ePaySimulator.queryRenewPayStatus(renewNo));
        assertEquals("renting", E2ePaySimulator.queryOrderStatus(orderNo));
        assertEquals(E2eServerSupport.M04_RENT_END_AFTER_RENEW,
                E2ePaySimulator.queryOrderEndTime(orderNo));
        assertTrue(E2ePaySimulator.countPaymentsByBizType(orderNo, "rent_renew") >= 1);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"returnMethod\":\"store\","
                        + "\"returnDate\":\"2026-09-18\",\"remark\":\"E2E归还\"}")
                .when()
                .post("/app/rental/return/apply")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("returning", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/return/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.returnMethod", equalTo("store"))
                .body("data.status", equalTo("pending"));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"deviceCondition\":\"good\","
                        + "\"deductAmount\":0,\"remark\":\"E2E验收\"}")
                .when()
                .post("/app/merchant/inspection/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("completed", E2ePaySimulator.queryOrderStatus(orderNo));

        Map<String, Object> appDetail = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("completed"))
                .body("data.logs.size()", greaterThan(0))
                .body("data.renewRecords.size()", greaterThan(0))
                .body("data.returnApply", notNullValue())
                .body("data.inspection.refundAmount", notNullValue())
                .extract()
                .path("data");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> appLogs = (List<Map<String, Object>>) appDetail.get("logs");
        assertTrue(appLogs.stream().anyMatch(log -> "renting".equals(log.get("toStatus"))
                        || "returning".equals(log.get("toStatus"))
                        || "completed".equals(log.get("toStatus"))),
                "用户端时间轴应含 renting/returning/completed 流转");

        assertAdminOrderConsistent(orderNo, orderId, token, "completed");
        assertAdminInspectionVisible(orderNo);
        assertNotNull(E2ePaySimulator.queryInspectionRefundAmount(orderNo));
    }

    @Test
    void inspectionRefundEqualsDepositMinusDeduct() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        String orderNo = (String) createResult.get("orderNo");
        long orderId = ((Number) createResult.get("orderId")).longValue();

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        confirmPickup(token, orderId);
        applyReturn(token, orderId);

        BigDecimal depositAmount = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.depositAmount");

        BigDecimal deduct = new BigDecimal("50.00");
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"deviceCondition\":\"minor_damage\","
                        + "\"deductAmount\":" + deduct + ",\"remark\":\"E2E扣款\"}")
                .when()
                .post("/app/merchant/inspection/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        BigDecimal expectedRefund = depositAmount.subtract(deduct).max(BigDecimal.ZERO);
        BigDecimal actualRefund = E2ePaySimulator.queryInspectionRefundAmount(orderNo);
        assertEquals(0, expectedRefund.compareTo(actualRefund));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.inspection.deductAmount", equalTo(50.0f))
                .body("data.inspection.refundAmount", equalTo(expectedRefund.floatValue()));
    }

    @Test
    void confirmPickupRejectsWhenAlreadyRenting() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        long orderId = ((Number) createResult.get("orderId")).longValue();
        String orderNo = (String) createResult.get("orderNo");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        confirmPickup(token, orderId);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .post("/app/rental/pickup/confirm/" + orderId)
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    @Test
    void applyReturnRejectsWhenNotRenting() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        long orderId = ((Number) createResult.get("orderId")).longValue();
        String orderNo = (String) createResult.get("orderNo");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"returnMethod\":\"store\"}")
                .when()
                .post("/app/rental/return/apply")
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    private static void confirmPickup(String token, long orderId) {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .post("/app/rental/pickup/confirm/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    private static void applyReturn(String token, long orderId) {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"returnMethod\":\"store\"}")
                .when()
                .post("/app/rental/return/apply")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    private static void assertAdminOrderConsistent(String orderNo, long orderId, String appToken,
                                                   String expectedStatus) {
        if (!E2eServerSupport.isAdminOrderApiAvailable() || !E2eServerSupport.isAdminLoginAvailable()) {
            assertEquals(expectedStatus, E2ePaySimulator.queryOrderStatus(orderNo));
            return;
        }

        String adminToken = E2eServerSupport.adminLoginToken();
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}")
                .when()
                .post("/admin/rental/order/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.status",
                        equalTo(expectedStatus));

        Map<String, Object> adminDetail = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .when()
                .get("/admin/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo(expectedStatus))
                .body("data.logs.size()", greaterThan(0))
                .extract()
                .path("data");

        Map<String, Object> appDetail = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");

        assertEquals(appDetail.get("status"), adminDetail.get("status"));
        assertEquals(appDetail.get("orderNo"), adminDetail.get("orderNo"));
    }

    private static void assertAdminInspectionVisible(String orderNo) {
        if (!E2eServerSupport.isAdminInspectionApiAvailable() || !E2eServerSupport.isAdminLoginAvailable()) {
            return;
        }

        String adminToken = E2eServerSupport.adminLoginToken();
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"keyword\":\"" + orderNo + "\"}")
                .when()
                .post("/admin/rental/inspection/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.refundAmount", notNullValue());
    }

    private static Map<String, Object> createStorePickupOrder(String token) {
        Number cartId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + E2eServerSupport.M03_RENT_START + "\",\"endTime\":\""
                        + E2eServerSupport.M03_RENT_END + "\",\"qty\":1}")
                .when()
                .post("/app/rental/cart/add")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> confirmPayload = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"cartIds\":[" + cartId.longValue() + "],\"pickupType\":\"store\"}")
                .when()
                .post("/app/rental/cart/checkout-preview")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.confirmPayload");

        return given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body(buildCreateOrderBody(confirmPayload))
                .when()
                .post("/app/rental/order/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("pending_payment"))
                .extract()
                .path("data");
    }

    @SuppressWarnings("unchecked")
    private static String buildCreateOrderBody(Map<String, Object> confirmPayload) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) confirmPayload.get("items");
        List<? extends Number> cartIds = (List<? extends Number>) confirmPayload.get("cartIds");
        String pickupType = String.valueOf(confirmPayload.get("pickupType"));

        StringBuilder itemsJson = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            if (i > 0) {
                itemsJson.append(',');
            }
            itemsJson.append("{\"goodsId\":").append(item.get("goodsId"))
                    .append(",\"specId\":").append(item.get("specId") == null ? 0 : item.get("specId"))
                    .append(",\"rentType\":\"").append(item.get("rentType")).append("\"")
                    .append(",\"startTime\":\"").append(item.get("startTime")).append("\"")
                    .append(",\"endTime\":\"").append(item.get("endTime")).append("\"")
                    .append(",\"quantity\":").append(item.get("quantity") == null ? 1 : item.get("quantity"))
                    .append('}');
        }
        itemsJson.append(']');

        StringBuilder cartIdsJson = new StringBuilder("[");
        if (cartIds != null) {
            for (int i = 0; i < cartIds.size(); i++) {
                if (i > 0) {
                    cartIdsJson.append(',');
                }
                cartIdsJson.append(cartIds.get(i).longValue());
            }
        }
        cartIdsJson.append(']');

        return "{\"items\":" + itemsJson
                + ",\"pickupType\":\"" + pickupType + "\""
                + ",\"contactName\":\"E2E-M04\",\"contactPhone\":\""
                + E2eServerSupport.TEST_PHONE + "\""
                + ",\"cartIds\":" + cartIdsJson + "}";
    }
}
