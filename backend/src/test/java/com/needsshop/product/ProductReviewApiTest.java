package com.needsshop.product;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 후기 API(읽기 전용): 인증 없이 호출, 최신순 페이징, size 상한, 없는 상품 404, 상세의 후기 요약 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductReviewApiTest {

    @Autowired TestRestTemplate rest;
    @Autowired ProductRepository products;
    @Autowired ReviewRepository reviews;
    @Autowired JdbcTemplate jdbc;

    private final List<Long> createdProducts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbc.execute("delete from product_review");
    }

    @AfterEach
    void tearDown() {
        // 다른 테스트가 product를 통째로 지우므로, FK가 걸린 후기를 남기지 않는다
        jdbc.execute("delete from product_review");
        createdProducts.forEach(id -> jdbc.update("delete from product where id = ?", id));
    }

    private Product product(String name) {
        Product p = products.save(Product.builder().name(name).subCategory(SubCategory.MOUSE_WIRELESS).price(10_000).stock(5).build());
        createdProducts.add(p.getId());
        return p;
    }

    /** 후기를 넣고 상품의 후기 수·별점 합계도 같이 맞춘다 (시드와 같은 방식) */
    private void addReviews(Product p, int... ratings) {
        LocalDateTime base = LocalDateTime.of(2026, 11, 20, 12, 0);
        int sum = 0;
        for (int i = 0; i < ratings.length; i++) {
            // i가 클수록 오래된 후기
            reviews.save(new Review(p.getId(), ratings[i], i % 3 == 0 ? null : "후기 " + i, "구매자 a" + i, base.minusDays(i)));
            sum += ratings[i];
        }
        jdbc.update("update product set review_count = ?, rating_sum = ? where id = ?", ratings.length, sum, p.getId());
    }

    private JsonNode get(String path) {
        ResponseEntity<JsonNode> res = rest.getForEntity(path, JsonNode.class);
        assertThat(res.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        return res.getBody();
    }

    @Test
    @DisplayName("후기는 최신순이고 페이지 정보(total, hasNext)와 날짜(yyyy-MM-dd)를 내려준다. 25개를 10개씩: 1페이지 10개(다음 있음), 3페이지 5개(다음 없음). 로그인 없이 볼 수 있다")
    void pages() {
        Product p = product("후기 상품");
        int[] ratings = new int[25];
        for (int i = 0; i < 25; i++) ratings[i] = 1 + i % 5;
        addReviews(p, ratings);

        JsonNode first = get("/api/products/" + p.getId() + "/reviews?page=0&size=10");
        assertThat(first.get("items")).hasSize(10);
        assertThat(first.get("page").asInt()).isZero();
        assertThat(first.get("size").asInt()).isEqualTo(10);
        assertThat(first.get("total").asLong()).isEqualTo(25);
        assertThat(first.get("hasNext").asBoolean()).isTrue();
        // 최신순: 가장 최근(i=0)이 먼저. i=0은 본문이 null인 후기, 날짜는 기준일
        JsonNode newest = first.get("items").get(0);
        assertThat(newest.get("rating").asInt()).isEqualTo(1);
        assertThat(newest.get("body").isNull()).isTrue();
        assertThat(newest.get("nickname").asText()).isEqualTo("구매자 a0");
        assertThat(newest.get("date").asText()).isEqualTo("2026-11-20");
        assertThat(first.get("items").get(1).get("body").asText()).isEqualTo("후기 1");
        assertThat(first.get("items").get(1).get("date").asText()).isEqualTo("2026-11-19");

        JsonNode last = get("/api/products/" + p.getId() + "/reviews?page=2&size=10");
        assertThat(last.get("items")).hasSize(5);
        assertThat(last.get("hasNext").asBoolean()).isFalse();
        assertThat(last.get("items").get(4).get("date").asText()).isEqualTo("2026-10-27");   // i=24

        JsonNode beyond = get("/api/products/" + p.getId() + "/reviews?page=9&size=10");
        assertThat(beyond.get("items")).isEmpty();
        assertThat(beyond.get("hasNext").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("size는 최대 20으로 제한하고, 기본값은 page=0 size=10. 이상한 값은 범위 안으로 맞춘다")
    void sizeCap() {
        Product p = product("후기 상품");
        int[] ratings = new int[30];
        java.util.Arrays.fill(ratings, 5);
        addReviews(p, ratings);

        JsonNode capped = get("/api/products/" + p.getId() + "/reviews?size=100");
        assertThat(capped.get("size").asInt()).isEqualTo(20);
        assertThat(capped.get("items")).hasSize(20);
        assertThat(capped.get("hasNext").asBoolean()).isTrue();

        JsonNode defaults = get("/api/products/" + p.getId() + "/reviews");
        assertThat(defaults.get("page").asInt()).isZero();
        assertThat(defaults.get("size").asInt()).isEqualTo(10);
        assertThat(defaults.get("items")).hasSize(10);

        JsonNode weird = get("/api/products/" + p.getId() + "/reviews?page=-3&size=0");
        assertThat(weird.get("page").asInt()).isZero();
        assertThat(weird.get("size").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 상품, 판매 중지 상품은 404 PRODUCT_NOT_FOUND(P001)이고 traceId가 따라온다")
    void notFound() {
        Product inactive = product("중지 상품");
        addReviews(inactive, 5, 4);
        jdbc.update("update product set active = false where id = ?", inactive.getId());

        for (String path : List.of("/api/products/999999999/reviews", "/api/products/" + inactive.getId() + "/reviews")) {
            ResponseEntity<JsonNode> res = rest.getForEntity(path, JsonNode.class);
            assertThat(res.getStatusCode()).as(path).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(res.getBody().get("code").asText()).isEqualTo("P001");
            assertThat(res.getBody().get("traceId").asText()).isNotBlank();
        }
    }

    @Test
    @DisplayName("상세 응답의 reviewSummary: 평균, 개수, 1~5점 분포")
    void detailSummary() {
        Product p = product("요약 상품");
        addReviews(p, 5, 5, 5, 4, 4, 3, 1);   // 합 27, 7개 -> 3.857 -> 3.9

        JsonNode detail = get("/api/products/" + p.getId());

        JsonNode summary = detail.get("reviewSummary");
        assertThat(summary.get("average").asDouble()).isEqualTo(3.9);
        assertThat(summary.get("count").asInt()).isEqualTo(7);
        JsonNode dist = summary.get("distribution");
        assertThat(dist.get("1").asInt()).isEqualTo(1);
        assertThat(dist.get("2").asInt()).isZero();
        assertThat(dist.get("3").asInt()).isEqualTo(1);
        assertThat(dist.get("4").asInt()).isEqualTo(2);
        assertThat(dist.get("5").asInt()).isEqualTo(3);
        // 목록 카드용 값도 같이 내려온다 (최상위 필드)
        assertThat(detail.get("ratingAvg").asDouble()).isEqualTo(3.9);
        assertThat(detail.get("reviewCount").asInt()).isEqualTo(7);
        assertThat(detail.get("badges").isArray()).isTrue();
    }

    @Test
    @DisplayName("후기가 없어도 reviewSummary에 distribution 1~5가 0으로 채워져 항상 들어 있다")
    void detailSummaryWithoutReviews() {
        Product p = product("후기 없는 상품");

        JsonNode summary = get("/api/products/" + p.getId()).get("reviewSummary");

        assertThat(summary.get("average").asDouble()).isZero();
        assertThat(summary.get("count").asInt()).isZero();
        for (String star : List.of("1", "2", "3", "4", "5")) {
            assertThat(summary.get("distribution").has(star)).as(star + "점 키").isTrue();
            assertThat(summary.get("distribution").get(star).asInt()).isZero();
        }
        assertThat(get("/api/products/" + p.getId() + "/reviews").get("items")).isEmpty();
    }

    @Test
    @DisplayName("목록 응답에 ratingAvg, reviewCount, badges가 들어 있다 (후기 없는 상품은 0, 0, [])")
    void listCardFields() {
        Product p = product("카드 상품");
        addReviews(p, 5, 4);

        JsonNode list = get("/api/products");

        JsonNode card = null;
        for (JsonNode n : list) if (n.get("id").asLong() == p.getId()) card = n;
        assertThat(card).isNotNull();
        assertThat(card.get("ratingAvg").asDouble()).isEqualTo(4.5);
        assertThat(card.get("reviewCount").asInt()).isEqualTo(2);
        assertThat(card.get("badges").isArray()).isTrue();
    }
}
