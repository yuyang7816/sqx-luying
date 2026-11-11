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
 * M07 E2E：入驻进度→商品/门店→C 端下单支付→LINK 订单同步→备货核销→归还验收→admin 可查。
 * <p>
 * 对齐需求文档模块七与管理后台 9.2 场景 5-6。
 * 依赖 server:9135 与 MySQL 种子链（含 m07_merchant.sql / m07_merchant_seed.sql）。
 */
@Tag("e2e")
class M07MerchantE2ETest {

    @BeforeAll
    static void assumeServerAndM07ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，请确认 E2E_MYSQL_* 与 m07 迁移");
        E2eM07SeedSupport.ensureMerchantSeedIfMissing();
        assumeTrue(E2eServerSupport.isM07MerchantApiAvailable(),
                () -> "E2E 跳过：M07 商户接口未部署，请重启 server 至最新代码");
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：M03 购物车/订单接口未部署");
        assumeTrue(E2eServerSupport.isM04OrderLifecycleApiAvailable(),
                () -> "E2E 跳过：M04 归还/验收接口未部署");
    }

    @Test
    void merchantSeedProgressGoodsAndStore() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String merchantToken = E2eServerSupport.merchantLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .get("/app/merchant/apply/progress")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("approved"))
                .body("data.merchantName", notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10}")
                .when()
                .post("/app/merchant/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.size()", greaterThan(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .get("/app/merchant/store/detail")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.name", notNullValue())
                .body("data.staffList.size()", greaterThan(0));
    }

    @Test
    void merchantFullChainLinkOrderPreparePickupReturnInspection() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法模拟支付");

        String customerToken = E2eServerSupport.appLoginToken();
        String merchantToken = E2eServerSupport.merchantLoginToken();

        Map<String, Object> createResult = createM07StorePickupOrder(customerToken);
        String orderNo = (String) createResult.get("orderNo");
        long orderId = ((Number) createResult.get("orderId")).longValue();
        String linkId = "LINK-" + orderId;

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);
        assertEquals("waiting_pickup", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"tab\":\"pending\"}")
                .when()
                .post("/app/merchant/order/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.merchantOrderNo == '" + linkId + "' }.merchantOrderNo",
                        equalTo(linkId));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .get("/app/merchant/order/detail/" + linkId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.nextAction", equalTo("prepare"));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .post("/app/merchant/order/prepare/" + linkId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        String pickupCode = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .when()
                .get("/app/rental/pickup/code/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.pickupCode");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"pickupCode\":\"" + pickupCode + "\"}")
                .when()
                .post("/app/merchant/order/pickup/" + linkId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("renting", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"returnMethod\":\"store\",\"returnDate\":\"2026-12-03\"}")
                .when()
                .post("/app/rental/return/apply")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("returning", E2ePaySimulator.queryOrderStatus(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .get("/app/merchant/order/detail/" + linkId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.nextAction", equalTo("inspection"));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"orderId\":" + orderId + ",\"deviceCondition\":\"good\","
                        + "\"deductAmount\":0,\"remark\":\"E2E-M07验收\"}")
                .when()
                .post("/app/merchant/inspection/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        assertEquals("completed", E2ePaySimulator.queryOrderStatus(orderNo));
        assertNotNull(E2ePaySimulator.queryInspectionRefundAmount(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .when()
                .get("/app/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("completed"))
                .body("data.inspection.refundAmount", notNullValue());

        assertAdminMerchantAndInspectionVisible(orderNo, orderId);
    }

    @Test
    void merchantImConversationAndSubscribeTemplates() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String customerToken = E2eServerSupport.appLoginToken();
        String merchantToken = E2eServerSupport.merchantLoginToken();

        Number merchantId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .when()
                .get("/app/merchant/apply/progress")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.merchantId");

        Number conversationId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .when()
                .get("/app/im/conversation/open/" + merchantId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .extract()
                .path("data.id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .contentType(ContentType.JSON)
                .body("{\"conversationId\":" + conversationId.longValue() + ",\"content\":\"E2E-M07-IM-用户\"}")
                .when()
                .post("/app/im/message/send")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/app/merchant/messages/unread-summary")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.imUnread", greaterThan(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10}")
                .when()
                .post("/app/merchant/im/conversation/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.size()", greaterThan(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, merchantToken)
                .contentType(ContentType.JSON)
                .body("{\"conversationId\":" + conversationId.longValue() + ",\"content\":\"E2E-M07-IM-商户\"}")
                .when()
                .post("/app/merchant/im/message/send")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, customerToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/app/messages/subscribe-templates")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.orderNotify", notNullValue());
    }

    @Test
    void adminMerchantApplyPageListsRecords() {
        assumeTrue(E2eServerSupport.isAdminMerchantApiAvailable(),
                () -> "E2E 跳过：admin 商户入驻接口未部署");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达");

        String adminToken = E2eServerSupport.adminLoginToken();
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20}")
                .when()
                .post("/admin/merchant/apply/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20}")
                .when()
                .post("/admin/merchant/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.merchantNo == '" + E2eServerSupport.M07_SEED_MERCHANT_NO + "' }.name",
                        notNullValue());
    }

    private static Map<String, Object> createM07StorePickupOrder(String token) {
        Number cartId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + E2eServerSupport.M07_RENT_START + "\",\"endTime\":\""
                        + E2eServerSupport.M07_RENT_END + "\",\"qty\":1}")
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
                + ",\"contactName\":\"E2E-M07\",\"contactPhone\":\""
                + E2eServerSupport.TEST_PHONE + "\""
                + ",\"cartIds\":" + cartIdsJson + "}";
    }

    private static void assertAdminMerchantAndInspectionVisible(String orderNo, long orderId) {
        if (!E2eServerSupport.isAdminOrderApiAvailable()
                || !E2eServerSupport.isAdminInspectionApiAvailable()
                || !E2eServerSupport.isAdminLoginAvailable()) {
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
                        equalTo("completed"));

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

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .when()
                .get("/admin/rental/order/detail/" + orderId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("completed"));
    }
}
