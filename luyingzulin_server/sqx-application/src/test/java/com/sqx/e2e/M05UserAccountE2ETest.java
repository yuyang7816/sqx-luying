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
 * M05 E2E：登录回跳预订、地址结算回写、收藏同步、admin 用户列表。
 * <p>
 * 对齐 uniapp 游客详情点预订→登录→继续预订；新增地址→确认订单选地址；
 * 详情收藏→列表可见→取消同步；admin 展示演示用户数据。
 * 依赖 server:9135 与 MySQL 种子链（含 m05_user_account.sql / m05_user_account_seed.sql）。
 */
@Tag("e2e")
class M05UserAccountE2ETest {

    /** 种子未收藏商品（m05 种子收藏 1/2/4） */
    private static final long M05_TOGGLE_GOODS_ID = 3L;
    private static final String E2E_ADDRESS_NAME = "E2E-M05-收货人";
    private static final String E2E_ADDRESS_PHONE = "13900005555";

    @BeforeAll
    static void assumeServerAndM05ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM05UserAccountApiAvailable(),
                () -> "E2E 跳过：M05 用户账户接口未部署，请重启 server（需 GET /app/user/favorite/list）");
        assumeTrue(E2eServerSupport.isM02GoodsBrowseApiAvailable(),
                () -> "E2E 跳过：商品详情接口未部署（需 GET /app/rental/goods/detail）");
        assumeTrue(E2eServerSupport.isM03CartOrderApiAvailable(),
                () -> "E2E 跳过：购物车接口未部署（需 POST /app/rental/cart/add）");
    }

    @Test
    void guestDetailBookRequiresLoginThenContinueAfterM05Auth() {
        long goodsId = E2eServerSupport.M03_GOODS_ID;

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/goods/detail/" + goodsId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", equalTo((int) goodsId))
                .body("data.name", notNullValue())
                .body("data.goodsNo", notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + goodsId
                        + ",\"specId\":0,\"rentType\":\"day\",\"startTime\":\""
                        + E2eServerSupport.M03_RENT_START + "\",\"endTime\":\""
                        + E2eServerSupport.M03_RENT_END + "\",\"qty\":1}")
                .when()
                .post("/app/rental/cart/add")
                .then()
                .statusCode(200)
                .body("code", equalTo(401));

        String token = E2eServerSupport.appLoginTokenViaM05Auth();

        Number cartId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + goodsId
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
                        equalTo((int) goodsId));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .delete("/app/rental/cart/" + cartId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void newAddressCheckoutPreviewWriteBackAddressId() {
        String token = E2eServerSupport.appLoginToken();

        Number addressId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"contactName\":\"" + E2E_ADDRESS_NAME + "\","
                        + "\"contactPhone\":\"" + E2E_ADDRESS_PHONE + "\","
                        + "\"province\":\"陕西省\",\"city\":\"西安市\",\"district\":\"未央区\","
                        + "\"detailAddress\":\"E2E 测试路 88 号\",\"isDefault\":false}")
                .when()
                .post("/app/user/address/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.contactName", equalTo(E2E_ADDRESS_NAME))
                .body("data.contactPhone", equalTo(E2E_ADDRESS_PHONE))
                .extract()
                .path("data.id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/address/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.find { it.id == " + addressId.longValue() + " }.contactName",
                        equalTo(E2E_ADDRESS_NAME));

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

        Map<String, Object> preview = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"cartIds\":[" + cartId.longValue() + "],"
                        + "\"pickupType\":\"delivery\",\"addressId\":" + addressId.longValue() + "}")
                .when()
                .post("/app/rental/cart/checkout-preview")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.addressId", equalTo(addressId.intValue()))
                .body("data.confirmPayload.addressId", equalTo(addressId.intValue()))
                .body("data.pickupType", equalTo("delivery"))
                .extract()
                .path("data");

        @SuppressWarnings("unchecked")
        Map<String, Object> confirmPayload = (Map<String, Object>) preview.get("confirmPayload");
        assertEquals(addressId.longValue(), ((Number) confirmPayload.get("addressId")).longValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .delete("/app/user/address/delete/" + addressId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .delete("/app/rental/cart/" + cartId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void favoriteToggleSyncListAndCheck() {
        String token = E2eServerSupport.appLoginToken();
        ensureNotFavorite(token, M05_TOGGLE_GOODS_ID);

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/check?goodsId=" + M05_TOGGLE_GOODS_ID)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", equalTo(false));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + M05_TOGGLE_GOODS_ID + "}")
                .when()
                .post("/app/user/favorite/toggle")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.favorite", equalTo(true))
                .body("data.favoriteCount", greaterThan(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/check?goodsId=" + M05_TOGGLE_GOODS_ID)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", equalTo(true));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.find { it.goodsId == " + M05_TOGGLE_GOODS_ID + " }.goodsName", notNullValue())
                .body("data.find { it.goodsId == " + M05_TOGGLE_GOODS_ID + " }.goodsNo", notNullValue());

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":" + M05_TOGGLE_GOODS_ID + "}")
                .when()
                .post("/app/user/favorite/toggle")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.favorite", equalTo(false));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/check?goodsId=" + M05_TOGGLE_GOODS_ID)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", equalTo(false));

        List<Map<String, Object>> favorites = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");

        assertTrue(favorites.stream().noneMatch(item ->
                M05_TOGGLE_GOODS_ID == ((Number) item.get("goodsId")).longValue()));
    }

    private static void ensureNotFavorite(String token, long goodsId) {
        Boolean favorited = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/favorite/check?goodsId=" + goodsId)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data");
        if (Boolean.TRUE.equals(favorited)) {
            given()
                    .baseUri(E2eServerSupport.BASE_URL)
                    .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                    .contentType(ContentType.JSON)
                    .body("{\"goodsId\":" + goodsId + "}")
                    .when()
                    .post("/app/user/favorite/toggle")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .body("data.favorite", equalTo(false));
        }
    }

    @Test
    void adminUserListShowsDemoUserWithPlainPhone() {
        assumeTrue(E2eServerSupport.isAdminUserApiAvailable(),
                () -> "E2E 跳过：admin 用户列表接口未部署（需 POST /admin/user/list）");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        String adminToken = E2eServerSupport.adminLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"phone\":\"13812348888\"}")
                .when()
                .post("/admin/user/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", not(empty()))
                .body("data.records.find { it.invitationCode == 'CAMP001' }.phone", equalTo("13812348888"))
                .body("data.records.find { it.invitationCode == 'CAMP001' }.phoneMasked", notNullValue())
                .body("data.records.find { it.invitationCode == 'CAMP001' }.phoneMasked",
                        equalTo("138****8888"))
                .body("data.records.find { it.invitationCode == 'CAMP001' }.status", equalTo(1))
                .body("data.records.find { it.invitationCode == 'CAMP001' }.userName", notNullValue());

        Number userId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":1,\"phone\":\"13812348888\"}")
                .when()
                .post("/admin/user/list")
                .then()
                .statusCode(200)
                .extract()
                .path("data.records.find { it.invitationCode == 'CAMP001' }.userId");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .when()
                .get("/admin/user/info?userId=" + userId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.invitationCode", equalTo("CAMP001"))
                .body("data.phone", equalTo("13812348888"))
                .body("data.phoneMasked", equalTo("138****8888"));
    }

    @Test
    void appUserInfoReturnsMaskedPhoneOnly() {
        String token = E2eServerSupport.appLoginTokenViaM05Auth();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/user/info")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.phoneMasked", equalTo("138****8888"))
                .body("data.phone", nullValue());
    }

    @Test
    void adminOperationalPagesReturnPlainPhoneWhenM06SeedPresent() {
        assumeTrue(E2eServerSupport.isAdminM06AssetsApiAvailable(),
                () -> "E2E 跳过：M06 运营资产接口未部署");
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法 admin 登录");

        String adminToken = E2eServerSupport.adminLoginToken();
        String testPhone = E2eServerSupport.TEST_PHONE;

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"phone\":\"" + testPhone + "\"}")
                .when()
                .post("/admin/user/deposit/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.phone == '" + testPhone + "' }.phone",
                        equalTo(testPhone));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"phone\":\"" + testPhone + "\"}")
                .when()
                .post("/admin/user/feedback/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.feedbackNo == '" + E2eServerSupport.M06_SEED_FEEDBACK_NO + "' }.phone",
                        equalTo(testPhone));

        Number feedbackId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"phone\":\"" + testPhone + "\"}")
                .when()
                .post("/admin/user/feedback/page")
                .then()
                .statusCode(200)
                .extract()
                .path("data.records.find { it.feedbackNo == '" + E2eServerSupport.M06_SEED_FEEDBACK_NO + "' }.id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .when()
                .get("/admin/user/feedback/info?id=" + feedbackId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.phone", equalTo(testPhone));

        Number userId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":1,\"phone\":\"" + testPhone + "\"}")
                .when()
                .post("/admin/user/list")
                .then()
                .statusCode(200)
                .extract()
                .path("data.records[0].userId");

        List<Map<String, Object>> walletRecords = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"userId\":" + userId.longValue() + "}")
                .when()
                .post("/admin/userWalletTransaction/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .jsonPath()
                .getList("data.records");
        assertPlainPhoneWhenRecordsPresent(walletRecords, testPhone);

        List<Map<String, Object>> withdrawRecords = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"userId\":" + userId.longValue() + "}")
                .when()
                .post("/admin/withdrawRecord/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .jsonPath()
                .getList("data.records");
        assertPlainPhoneWhenRecordsPresent(withdrawRecords, testPhone);

        List<Map<String, Object>> accountRecords = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"userId\":" + userId.longValue() + ",\"source\":1}")
                .when()
                .post("/admin/withdrawalAccount/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .jsonPath()
                .getList("data.records");
        assertPlainPhoneWhenRecordsPresent(accountRecords, testPhone);

        List<Map<String, Object>> messageRecords = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"phone\":\"" + testPhone + "\"}")
                .when()
                .post("/admin/messages/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .jsonPath()
                .getList("data.records");
        assertPlainPhoneWhenRecordsPresent(messageRecords, testPhone);
    }

    private static void assertPlainPhoneWhenRecordsPresent(List<Map<String, Object>> records, String expectedPhone) {
        if (records == null || records.isEmpty()) {
            return;
        }
        boolean matched = records.stream()
                .map(record -> record.get("phone"))
                .filter(phone -> phone != null)
                .anyMatch(expectedPhone::equals);
        assertTrue(matched, "有记录时运营页应返回明文手机号 " + expectedPhone);
    }
}
