package com.sqx.e2e;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * M00 E2E：用户端登录与验证码链路（RestAssured）
 * <p>
 * 任务口径 /app/auth/login、/app/verifyCode 对应实现路径：
 * {@code POST /app/auth/phonePasswordLogin}（M05 别名）、
 * {@code POST /app/base/auth/phonePasswordLogin}、
 * {@code POST /app/base/verifyCode/sendPhoneCode}。
 * 服务不可达时自动跳过，不影响 {@code mvn test} 其他用例。
 */
@Tag("e2e")
class AppAuthE2ETest {

    @BeforeAll
    static void assumeServerReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
    }

    @Test
    void verifyCodeSendThenPhoneCodeLogin() {
        Response sendResponse = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phone\":\"" + E2eServerSupport.TEST_PHONE + "\",\"bizCode\":\"login\"}")
                .when()
                .post("/app/base/verifyCode/sendPhoneCode")
                .then()
                .statusCode(200)
                .extract()
                .response();

        int sendCode = sendResponse.path("code");
        assumeTrue(sendCode == 0, () -> "E2E 跳过：短信验证码不可用 code=" + sendCode + "（本地未配置短信平台）");

        String verifyCode = sendResponse.path("data.code");
        assumeTrue(verifyCode != null && !verifyCode.trim().isEmpty(), "E2E 跳过：未返回验证码");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + E2eServerSupport.TEST_PHONE + "\",\"verifyCode\":\"" + verifyCode + "\"}")
                .when()
                .post("/app/base/auth/phoneCodeLogin")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .body("data.user.phone", equalTo(E2eServerSupport.TEST_PHONE));
    }

    @Test
    void phonePasswordLoginReturnsToken() {
        String token = loginAndExtractToken("/app/base/auth/phonePasswordLogin");
        assertFalse(token.trim().isEmpty(), "登录 token 不应为空");
        assertAuthedCategoryTree(token);
    }

    /** M05 别名路径：任务口径 /app/auth/login → phonePasswordLogin */
    @Test
    void appAuthAliasPhonePasswordLoginReturnsToken() {
        String token = loginAndExtractToken("/app/auth/phonePasswordLogin");
        assertFalse(token.trim().isEmpty(), "别名登录 token 不应为空");
        assertAuthedCategoryTree(token);
    }

    private static String loginAndExtractToken(String loginPath) {
        return given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"phoneNo\":\"" + E2eServerSupport.TEST_PHONE + "\",\"password\":\"" + E2eServerSupport.TEST_PASSWORD + "\"}")
                .when()
                .post(loginPath)
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .extract()
                .path("data.token");
    }

    private static void assertAuthedCategoryTree(String token) {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/category/tree")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }
}
