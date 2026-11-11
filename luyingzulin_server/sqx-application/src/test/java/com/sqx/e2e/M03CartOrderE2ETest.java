package com.sqx.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * M03 E2E：加购→结算→下单→支付→订单状态；取消待支付；库存冲突。
 * <p>
 * 对齐《后端接口开发改造计划》1.5 联调清单用例 1/2/4/5。
 * 依赖 server:9135 与 MySQL 种子链（含 m03_cart_order_seed.sql）。
 */
@Tag("e2e")
class M03CartOrderE2ETest {

    @BeforeAll
    static void assumeServerAndM03ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：M03 购物车/订单接口未部署，请重启 server（需 GET /app/rental/cart/list）");
    }

    @Test
    void addCartCheckoutCreateOrderFullChain() {
        String token = E2eServerSupport.appLoginToken();

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
                .body("data", notNullValue())
                .extract()
                .path("data");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/cart/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.find { it.id == " + cartId.longValue() + " }.goodsId",
                        equalTo((int) E2eServerSupport.M03_GOODS_ID));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"ids\":[" + cartId.longValue() + "],\"selected\":true}")
                .when()
                .put("/app/rental/cart/select")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        Map<String, Object> preview = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"cartIds\":[" + cartId.longValue() + "],\"pickupType\":\"store\"}")
                .when()
                .post("/app/rental/cart/checkout-preview")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.payAmount", notNullValue())
                .body("data.confirmPayload", notNullValue())
                .body("data.confirmPayload.items.size()", greaterThan(0))
                .extract()
                .path("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> confirmPayload = (Map<String, Object>) preview.get("confirmPayload");
        assertNotNull(confirmPayload.get("items"));

        Map<String, Object> createResult = given()
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
                .body("data.orderNo", notNullValue())
                .body("data.payAmount", notNullValue())
                .extract()
                .path("data");

        String orderNo = (String) createResult.get("orderNo");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/cart/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.find { it.id == " + cartId.longValue() + " }", nullValue());

        assertOrderVisibleForAdminOrDatabase(orderNo, "pending_payment", token);
    }

    @Test
    void paySuccessUpdatesStatusToWaitingPickupWithPickupCode() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法模拟支付回调");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        String orderNo = (String) createResult.get("orderNo");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/pay-status/" + orderNo)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("waiting_pickup"))
                .body("data.paid", equalTo(true))
                .body("data.pickupCode", notNullValue());

        assertEquals("waiting_pickup", E2ePaySimulator.queryOrderStatus(orderNo));
        assertOrderVisibleForAdminOrDatabase(orderNo, "waiting_pickup", token);
    }

    @Test
    void createOrderFailsWhenStockConflictInM02Window() {
        String token = E2eServerSupport.appLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"items\":[{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + E2eServerSupport.M02_STOCK_START + "\",\"endTime\":\""
                        + E2eServerSupport.M02_STOCK_END + "\",\"quantity\":11}],"
                        + "\"pickupType\":\"store\",\"contactName\":\"E2E冲突\","
                        + "\"contactPhone\":\"" + E2eServerSupport.TEST_PHONE + "\"}")
                .when()
                .post("/app/rental/order/create")
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)))
                .body("msg", containsString("库存不足"));
    }

    @Test
    void cancelPendingPaymentReleasesStockPrelock() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法校验库存预占释放");

        String token = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(token);
        String orderNo = (String) createResult.get("orderNo");
        Number orderId = (Number) createResult.get("orderId");

        assertEquals("PRELOCK", E2ePaySimulator.queryStockOccupyStatus(orderId.longValue()));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .post("/app/rental/order/cancel/" + orderId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/pay-status/" + orderNo)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("cancelled"))
                .body("data.paid", equalTo(false));

        assertEquals("RELEASED", E2ePaySimulator.queryStockOccupyStatus(orderId.longValue()));
        assertEquals("cancelled", E2ePaySimulator.queryOrderStatus(orderNo));
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
        @SuppressWarnings("unchecked")
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
                + ",\"contactName\":\"E2E测试\",\"contactPhone\":\""
                + E2eServerSupport.TEST_PHONE + "\""
                + ",\"cartIds\":" + cartIdsJson + "}";
    }

    private static void assertOrderVisibleForAdminOrDatabase(String orderNo, String expectedStatus, String appToken) {
        if (E2eServerSupport.isAdminOrderApiAvailable() && E2eServerSupport.isAdminLoginAvailable()) {
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
            return;
        }

        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：Admin 订单 API 未部署且 MySQL 不可达");
        assertEquals(expectedStatus, E2ePaySimulator.queryOrderStatus(orderNo));
    }
}
