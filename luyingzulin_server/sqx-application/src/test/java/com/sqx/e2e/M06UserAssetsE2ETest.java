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
 * M06 E2E：优惠券发放→下单抵扣→核销；钱包提现→admin 审核；反馈提交→admin 处理；押金流水与订单关联。
 * <p>
 * 对齐管理后台 9.2 场景 3-4 与 uniapp 券包/钱包/反馈链路。
 * 依赖 server:9135 与 MySQL 种子链（含 m06_user_assets.sql / m06_user_assets_seed.sql）。
 */
@Tag("e2e")
class M06UserAssetsE2ETest {

    private static final String E2E_FEEDBACK_CONTENT = "E2E-M06-反馈内容-" + System.currentTimeMillis();
    private static final BigDecimal E2E_WITHDRAW_AMOUNT = new BigDecimal("10.00");

    @BeforeAll
    static void assumeServerAndM06ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM06UserAssetsApiAvailable(),
                () -> "E2E 跳过：M06 用户资产接口未部署，请重启 server（需 GET /app/user/deposit/summary）");
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：购物车接口未部署（需 POST /app/rental/cart/add）");
    }

    @Test
    void seedCouponsDepositAndWalletMatchM06Baseline() {
        String token = E2eServerSupport.appLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"status\":\"unused\"}")
                .when()
                .post("/app/user/coupon/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.couponNo == '" + E2eServerSupport.M06_SEED_COUPON_NO + "' }.status",
                        equalTo("unused"))
                .body("data.records.find { it.couponNo == '" + E2eServerSupport.M06_SEED_COUPON_NO + "' }.name",
                        notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/deposit/summary")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.totalAmount", equalTo(200.0f))
                .body("data.frozenAmount", notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20}")
                .when()
                .post("/app/user/deposit/log/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.logNo == '" + E2eServerSupport.M06_SEED_DEPOSIT_LOG_NO + "' }.logType",
                        equalTo("pay"));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/userWallet/wallet")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.amount", notNullValue());
    }

    @Test
    void adminGrantCouponThenAppReceivesAndCheckoutConsume() {
        assumeTrue(E2eServerSupport.isAdminM06AssetsApiAvailable(),
                () -> "E2E 跳过：admin 券模板接口未部署");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        String adminToken = E2eServerSupport.adminLoginToken();
        String appToken = E2eServerSupport.appLoginToken();

        Number templateId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"templateNo\":\"" + E2eServerSupport.M06_TEMPLATE_NO + "\"}")
                .when()
                .post("/admin/coupon/template/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.templateNo == '" + E2eServerSupport.M06_TEMPLATE_NO + "' }.id",
                        notNullValue())
                .extract()
                .path("data.records.find { it.templateNo == '" + E2eServerSupport.M06_TEMPLATE_NO + "' }.id");

        int beforeCount = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":50,\"status\":\"unused\"}")
                .when()
                .post("/app/user/coupon/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.total");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"templateId\":" + templateId.longValue()
                        + ",\"phones\":[\"" + E2eServerSupport.TEST_PHONE + "\"]}")
                .when()
                .post("/admin/coupon/grant")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.grantCount", greaterThan(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":50,\"status\":\"unused\"}")
                .when()
                .post("/app/user/coupon/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", greaterThan(beforeCount));

        Number couponId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/user/coupon/usable?rentAmount=250&deliveryAmount=0&depositAmount=100")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.find { it.templateNo == '" + E2eServerSupport.M06_TEMPLATE_NO + "' }.id", notNullValue())
                .extract()
                .path("data.find { it.templateNo == '" + E2eServerSupport.M06_TEMPLATE_NO + "' && it.status == 'unused' }.id");

        Number cartId = addCart(appToken, E2eServerSupport.M06_COUPON_RENT_START, E2eServerSupport.M06_COUPON_RENT_END);

        Map<String, Object> preview = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"cartIds\":[" + cartId.longValue() + "],\"pickupType\":\"store\",\"couponId\":"
                        + couponId.longValue() + "}")
                .when()
                .post("/app/rental/cart/checkout-preview")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.couponAmount", greaterThan(0.0f))
                .body("data.confirmPayload.couponId", equalTo(couponId.intValue()))
                .extract()
                .path("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> confirmPayload = (Map<String, Object>) preview.get("confirmPayload");
        String createBody = buildCreateOrderBody(confirmPayload, couponId.longValue());

        Map<String, Object> createResult = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body(createBody)
                .when()
                .post("/app/rental/order/create")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("pending_payment"))
                .body("data.orderNo", notNullValue())
                .extract()
                .path("data");

        String orderNo = (String) createResult.get("orderNo");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":50,\"status\":\"used\"}")
                .when()
                .post("/app/user/coupon/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.id == " + couponId.longValue() + " }.status", equalTo("used"))
                .body("data.records.find { it.id == " + couponId.longValue() + " }.orderNo", equalTo(orderNo));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .post("/app/rental/order/cancel/" + createResult.get("orderId"))
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void walletWithdrawThenAdminConfirmUpdatesStatus() {
        assumeTrue(E2eServerSupport.isAdminM06AssetsApiAvailable(),
                () -> "E2E 跳过：admin 提现接口未部署");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        String appToken = E2eServerSupport.appLoginToken();
        String adminToken = E2eServerSupport.adminLoginToken();

        Number walletBefore = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/userWallet/wallet")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.amount");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"accountName\":\"E2E-M06\",\"accountNumber\":\"e2e_m06@alipay.com\",\"type\":1}")
                .when()
                .post("/app/withdrawalAccount/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/withdrawalAccount/getUserWithdrawalAccount?type=1")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.accountName", equalTo("E2E-M06"))
                .body("data.accountNumber", equalTo("e2e_m06@alipay.com"))
                .body("data.type", equalTo(1));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"amount\":" + E2E_WITHDRAW_AMOUNT
                        + ",\"accountType\":1,\"sourceType\":2}")
                .when()
                .post("/app/withdrawRecord/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        Number walletAfterApply = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/userWallet/wallet")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.amount");

        assertTrue(walletBefore.doubleValue() - walletAfterApply.doubleValue() >= E2E_WITHDRAW_AMOUNT.doubleValue() - 0.01,
                "提现申请应扣减钱包余额");

        Number withdrawId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":5}")
                .when()
                .post("/app/withdrawRecord/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records[0].status", equalTo(1))
                .body("data.records[0].amount", equalTo(E2E_WITHDRAW_AMOUNT.floatValue()))
                .extract()
                .path("data.records[0].id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10}")
                .when()
                .post("/admin/withdrawRecord/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.id == " + withdrawId.longValue() + " }.status", equalTo(1))
                .body("data.records.find { it.id == " + withdrawId.longValue() + " }.phone",
                        equalTo(E2eServerSupport.TEST_PHONE));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + withdrawId.longValue() + ",\"status\":2}")
                .when()
                .post("/admin/withdrawRecord/confirm")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":5}")
                .when()
                .post("/app/withdrawRecord/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.id == " + withdrawId.longValue() + " }.status", equalTo(2));
    }

    @Test
    void feedbackSubmitThenAdminVisibleAndProcess() {
        assumeTrue(E2eServerSupport.isAdminM06AssetsApiAvailable(),
                () -> "E2E 跳过：admin 反馈接口未部署");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        String appToken = E2eServerSupport.appLoginToken();
        String adminToken = E2eServerSupport.adminLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"feedbackType\":\"suggestion\",\"content\":\"" + E2E_FEEDBACK_CONTENT
                        + "\",\"contactPhone\":\"" + E2eServerSupport.TEST_PHONE + "\"}")
                .when()
                .post("/app/user/feedback/submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"status\":\"pending\"}")
                .when()
                .post("/admin/user/feedback/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.content == '" + E2E_FEEDBACK_CONTENT + "' }.feedbackNo", notNullValue())
                .body("data.records.find { it.content == '" + E2E_FEEDBACK_CONTENT + "' }.phone",
                        equalTo(E2eServerSupport.TEST_PHONE))
                .body("data.records.find { it.feedbackNo == '" + E2eServerSupport.M06_SEED_FEEDBACK_NO + "' }.status",
                        equalTo("pending"));

        Number feedbackId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"status\":\"pending\"}")
                .when()
                .post("/admin/user/feedback/page")
                .then()
                .statusCode(200)
                .extract()
                .path("data.records.find { it.content == '" + E2E_FEEDBACK_CONTENT + "' }.id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + feedbackId.longValue() + ",\"replyRemark\":\"E2E-M06-已处理\"}")
                .when()
                .post("/admin/user/feedback/process")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .when()
                .get("/admin/user/feedback/info?id=" + feedbackId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.status", equalTo("processed"))
                .body("data.replyRemark", equalTo("E2E-M06-已处理"))
                .body("data.phone", equalTo(E2eServerSupport.TEST_PHONE));
    }

    @Test
    void orderPayCreatesDepositFreezeLogLinkedToOrderNo() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法模拟支付与校验押金流水");

        String appToken = E2eServerSupport.appLoginToken();
        Map<String, Object> createResult = createStorePickupOrder(appToken,
                E2eServerSupport.M06_DEPOSIT_RENT_START, E2eServerSupport.M06_DEPOSIT_RENT_END);
        String orderNo = (String) createResult.get("orderNo");

        E2ePaySimulator.simulateRentOrderPaySuccess(orderNo);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"logType\":\"freeze\",\"orderNo\":\"" + orderNo + "\"}")
                .when()
                .post("/app/user/deposit/log/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.logType", equalTo("freeze"))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.amount", greaterThan(0.0f))
                .body("data.records.find { it.orderNo == '" + orderNo + "' }.status", equalTo("completed"));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, appToken)
                .when()
                .get("/app/user/deposit/summary")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.frozenAmount", greaterThan(0.0f));
    }

    private static Number addCart(String token, String startTime, String endTime) {
        return given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + E2eServerSupport.M03_GOODS_ID
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\"" + startTime
                        + "\",\"endTime\":\"" + endTime + "\",\"qty\":1}")
                .when()
                .post("/app/rental/cart/add")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");
    }

    private static Map<String, Object> createStorePickupOrder(String token, String startTime, String endTime) {
        Number cartId = addCart(token, startTime, endTime);

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
                .body(buildCreateOrderBody(confirmPayload, null))
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
    private static String buildCreateOrderBody(Map<String, Object> confirmPayload, Long couponId) {
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

        StringBuilder body = new StringBuilder("{\"items\":").append(itemsJson)
                .append(",\"pickupType\":\"").append(pickupType).append("\"")
                .append(",\"contactName\":\"E2E-M06\",\"contactPhone\":\"")
                .append(E2eServerSupport.TEST_PHONE).append("\"")
                .append(",\"cartIds\":").append(cartIdsJson);
        if (couponId != null) {
            body.append(",\"couponId\":").append(couponId);
        }
        body.append('}');
        return body.toString();
    }
}
