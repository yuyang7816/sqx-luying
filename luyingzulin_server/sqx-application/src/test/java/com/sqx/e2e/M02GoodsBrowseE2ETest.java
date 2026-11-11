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
 * M02 E2E：商品浏览 API 组合链路（搜索→列表→详情→库存校验）
 * <p>
 * 覆盖热门搜索词、关键词分页、详情规格/套餐、时段库存冲突。
 * 依赖运行中的 server 与 MySQL 种子（rental_v1.sql → m00 → m01 → m02_goods_browse_seed.sql）。
 */
@Tag("e2e")
class M02GoodsBrowseE2ETest {

    private static final String KEYWORD_TENT = "帐篷";
    private static final String M02_STOCK_START = "2026-08-10 10:00:00";
    private static final String M02_STOCK_END = "2026-08-12 18:00:00";

    @BeforeAll
    static void assumeServerAndM02ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM02GoodsBrowseApiAvailable(),
                () -> "E2E 跳过：M02 商品浏览接口未部署，请重启 server（需 GET /app/rental/search/hot）");
    }

    @Test
    void hotKeywordsIncludeTentKeywordFromSeed() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/search/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .body("data.size()", greaterThan(0))
                .body("data.keyword", hasItem(KEYWORD_TENT));
    }

    @Test
    void searchHotToListToDetailFullBrowseChain() {
        List<String> hotKeywords = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/search/hot")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .extract()
                .path("data.keyword");

        assertTrue(hotKeywords.contains(KEYWORD_TENT), "热门词应含种子关键词「帐篷」");

        List<Map<String, Object>> records = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"keyword\":\"" + KEYWORD_TENT + "\",\"rentType\":\"day\",\"sortBy\":\"default\"}")
                .when()
                .post("/app/rental/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .body("data.records.size()", greaterThan(0))
                .body("data.records[0].goodsNo", notNullValue())
                .body("data.records[0].name", notNullValue())
                .body("data.records[0].rentPrice", notNullValue())
                .extract()
                .path("data.records");

        Number goodsId = (Number) records.get(0).get("id");
        assertNotNull(goodsId, "列表首条应含商品 id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/goods/detail/" + goodsId.longValue())
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", equalTo(goodsId.intValue()))
                .body("data.name", notNullValue())
                .body("data.rentPrice", notNullValue())
                .body("data.specs", notNullValue())
                .body("data.specs.size()", greaterThan(0))
                .body("data.packages", notNullValue())
                .body("data.packages.size()", greaterThan(0));
    }

    @Test
    void detailReturnsM02SeedPackagesAndSpecsForFeeCalculation() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/goods/detail/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.rentPriceDay", notNullValue())
                .body("data.rentPriceHour", notNullValue())
                .body("data.depositAmount", notNullValue())
                .body("data.specs.find { it.name == '容纳人数' }.value", equalTo("3-4人"))
                .body("data.packages.find { it.packageName == '地钉防风绳加强包' }.price", equalTo(9.9f))
                .body("data.packages.find { it.packageName == '防潮垫双人款' }.price", equalTo(15.0f));
    }

    @Test
    void stockCheckAvailableWithinRemainAfterM02Occupy() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":1,\"specId\":0,\"startTime\":\"" + M02_STOCK_START
                        + "\",\"endTime\":\"" + M02_STOCK_END + "\",\"quantity\":10}")
                .when()
                .post("/app/rental/stock/check")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.available", equalTo(true))
                .body("data.remainStock", equalTo(10));
    }

    @Test
    void stockCheckConflictWhenQuantityExceedsRemain() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"goodsId\":1,\"specId\":0,\"startTime\":\"" + M02_STOCK_START
                        + "\",\"endTime\":\"" + M02_STOCK_END + "\",\"quantity\":11}")
                .when()
                .post("/app/rental/stock/check")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.available", equalTo(false))
                .body("data.remainStock", equalTo(10));
    }

    @Test
    void adminCreateGoodsThenOnShelfVisibleInUniappList() {
        assumeTrue(E2eServerSupport.isAdminLoginAvailable(),
                () -> "E2E 跳过：MySQL 不可达，无法完成管理端登录验证码流程");

        String adminToken = E2eServerSupport.adminLoginToken();
        String uniqueName = "E2E-M02-测试商品-" + System.currentTimeMillis();
        String uniqueGoodsNo = "RG-E2E-M02-" + System.currentTimeMillis();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"goodsNo\":\"" + uniqueGoodsNo + "\",\"name\":\"" + uniqueName
                        + "\",\"categoryId\":11,\"rentPriceDay\":19.90,\"depositAmount\":100.00,"
                        + "\"totalStock\":5,\"status\":0}")
                .when()
                .post("/admin/rental/goods/save")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        Number goodsId = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"keyword\":\"" + uniqueName + "\"}")
                .when()
                .post("/admin/rental/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.name == '" + uniqueName + "' }.id", notNullValue())
                .extract()
                .path("data.records.find { it.name == '" + uniqueName + "' }.id");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                .contentType(ContentType.JSON)
                .body("{\"id\":" + goodsId.longValue() + ",\"status\":1}")
                .when()
                .post("/admin/rental/goods/updateState")
                .then()
                .statusCode(200)
                .body("code", equalTo(0));

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"keyword\":\"" + uniqueName + "\",\"rentType\":\"day\",\"status\":1}")
                .when()
                .post("/app/rental/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records.find { it.name == '" + uniqueName + "' }.goodsNo", equalTo(uniqueGoodsNo));

        try {
            given()
                    .baseUri(E2eServerSupport.BASE_URL)
                    .header(E2eServerSupport.ADMIN_TOKEN_HEADER, adminToken)
                    .contentType(ContentType.JSON)
                    .body("{\"id\":" + goodsId.longValue() + "}")
                    .when()
                    .post("/admin/rental/goods/delete")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0));
        } catch (Exception ex) {
            // 清理失败不影响 E2E 断言结果
        }
    }
}
