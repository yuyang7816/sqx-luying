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
 * M01 E2E：TabBar 首页浏览 API 组合链路（RestAssured）
 * <p>
 * 覆盖 Banner 轮播、分类树、商品分页 rentType 筛选、登录后订单角标。
 * 依赖运行中的 server 与 MySQL 种子（rental_v1.sql + m01_tabbar_seed.sql）。
 */
@Tag("e2e")
class M01TabBarE2ETest {

    @BeforeAll
    static void assumeServerAndM01ApiReachable() {
        assumeTrue(E2eServerSupport.isServerReachable(),
                () -> "E2E 跳过：server 不可达 " + E2eServerSupport.BASE_URL);
        assumeTrue(E2eServerSupport.isM01TabBarApiAvailable(),
                () -> "E2E 跳过：M01 TabBar 接口未部署，请重启 server（需 GET /app/banner/page）");
    }

    @Test
    void bannerPageReturnsHomeCarouselStructure() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .queryParam("page", 1)
                .queryParam("limit", 10)
                .queryParam("classify", 1)
                .when()
                .get("/app/banner/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .body("data.records.size()", greaterThan(0))
                .body("data.records[0].name", notNullValue())
                .body("data.records[0].imageUrl", notNullValue())
                .body("data.records[0].state", equalTo(1));
    }

    @Test
    void categoryTreeThenGoodsPageByCategoryReturnsExpectedListVo() {
        List<Map<String, Object>> tree = given()
                .baseUri(E2eServerSupport.BASE_URL)
                .when()
                .get("/app/rental/category/tree")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .body("data.size()", greaterThan(0))
                .body("data[0].id", notNullValue())
                .body("data[0].name", notNullValue())
                .extract()
                .path("data");

        Long categoryId = resolveCategoryIdForGoodsFilter(tree);
        assertNotNull(categoryId, "分类树应包含可用于商品筛选的 categoryId");

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":10,\"categoryId\":" + categoryId + ",\"rentType\":\"day\"}")
                .when()
                .post("/app/rental/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .body("data.records.size()", greaterThan(0))
                .body("data.records[0].goodsNo", notNullValue())
                .body("data.records[0].name", notNullValue())
                .body("data.records[0].categoryName", notNullValue())
                .body("data.records[0].rentPrice", notNullValue())
                .body("data.records[0].categoryId", equalTo(categoryId.intValue()));
    }

    @Test
    void goodsPageRentTypeHourReturnsHourPrice() {
        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .contentType(ContentType.JSON)
                .body("{\"page\":1,\"limit\":20,\"rentType\":\"hour\"}")
                .when()
                .post("/app/rental/goods/page")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.records", notNullValue())
                .body("data.records.find { it.rentPriceHour != null }.rentPrice",
                        notNullValue());
    }

    @Test
    void orderStatusCountsReturnsM01SeedBadges() {
        String token = E2eServerSupport.appLoginToken();

        given()
                .baseUri(E2eServerSupport.BASE_URL)
                .header(E2eServerSupport.APP_TOKEN_HEADER, token)
                .when()
                .get("/app/rental/order/status-counts")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.pending_payment", greaterThanOrEqualTo(1))
                .body("data.waiting_pickup", greaterThanOrEqualTo(1))
                .body("data.renting", greaterThanOrEqualTo(1))
                .body("data.returning", greaterThanOrEqualTo(1))
                .body("data.after_sale", greaterThanOrEqualTo(1));
    }

    /**
     * 优先取子分类 ID（种子商品挂在二级分类如 11=帐篷睡袋），否则取根分类。
     */
    @SuppressWarnings("unchecked")
    private static Long resolveCategoryIdForGoodsFilter(List<Map<String, Object>> tree) {
        for (Map<String, Object> node : tree) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children != null && !children.isEmpty()) {
                Object childId = children.get(0).get("id");
                if (childId instanceof Number) {
                    return ((Number) childId).longValue();
                }
            }
        }
        Object rootId = tree.get(0).get("id");
        return rootId instanceof Number ? ((Number) rootId).longValue() : null;
    }
}
