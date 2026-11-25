package com.needsshop.idempotency;

import com.fasterxml.jackson.databind.JsonNode;
import com.needsshop.product.Product;
import com.needsshop.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestFixtures.class)
class OrderApiTest {

    @Autowired TestRestTemplate rest;
    @Autowired TestFixtures fixtures;

    private Product product;

    @BeforeEach
    void setUp() {
        fixtures.clean();
        product = fixtures.product(100);
    }

    @Test
    @DisplayName("같은 Idempotency-Key로 두 번 주문하면 주문은 1건이고, 두 번째는 저장된 응답을 재생한다")
    void replay() {
        String token = signup();
        String key = UUID.randomUUID().toString();

        ResponseEntity<JsonNode> first = order(token, key, product.getId(), 1);
        ResponseEntity<JsonNode> second = order(token, key, product.getId(), 1);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        assertThat(second.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(fixtures.count("orders")).isEqualTo(1);
        assertThat(fixtures.stockOf(product.getId())).isEqualTo(99);
    }

    @Test
    @DisplayName("같은 키로 다른 요청을 보내면 422")
    void keyReusedWithDifferentBody() {
        String token = signup();
        String key = UUID.randomUUID().toString();
        order(token, key, product.getId(), 1);

        ResponseEntity<JsonNode> res = order(token, key, product.getId(), 2);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().get("code").asText()).isEqualTo("I002");
    }

    @Test
    @DisplayName("따닥: 같은 키로 20개 요청이 동시에 와도 주문은 정확히 1건 생성된다")
    void doubleClick() throws Exception {
        String token = signup();
        String key = UUID.randomUUID().toString();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return order(token, key, product.getId(), 1);
            }));
        }
        start.countDown();
        List<HttpStatusCode> statuses = new ArrayList<>();
        for (var f : futures) statuses.add(f.get().getStatusCode());
        pool.shutdown();

        assertThat(fixtures.count("orders")).isEqualTo(1);
        assertThat(fixtures.stockOf(product.getId())).isEqualTo(99);
        // 먼저 도착한 1건만 201로 처리되고, 나머지는 201 재생 또는 409(처리 중)
        assertThat(statuses).allMatch(s -> s == HttpStatus.CREATED || s == HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("결제 따닥: 같은 주문에 서로 다른 키로 결제 10건이 동시에 와도 승인은 1건, 나머지는 409")
    void payDoubleClickWithDifferentKeys() throws Exception {
        // 화면 버튼 가드가 뚫려 클릭마다 새 키로 결제 요청이 나가는 경우. 멱등 키로는 같은 요청인지 알 수 없으므로
        // 주문 상태 전이(PENDING_PAYMENT → PAYING은 한 번만)와 @Version, 결제 키 유니크 제약이 막아야 한다
        String token = signup();
        long orderId = order(token, UUID.randomUUID().toString(), product.getId(), 1).getBody().get("id").asLong();

        int n = 10;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<JsonNode>>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return pay(token, UUID.randomUUID().toString(), orderId);
            }));
        }
        start.countDown();
        List<ResponseEntity<JsonNode>> responses = new ArrayList<>();
        for (var f : futures) responses.add(f.get());
        pool.shutdown();

        List<HttpStatusCode> statuses = responses.stream().map(ResponseEntity::getStatusCode).toList();
        assertThat(statuses).as("응답 코드").filteredOn(s -> s == HttpStatus.OK).hasSize(1);
        assertThat(statuses).as("나머지는 사용자에게 500이 아니라 409").filteredOn(s -> s != HttpStatus.OK)
                .allMatch(s -> s == HttpStatus.CONFLICT);
        assertThat(fixtures.count("payment")).as("PG를 부르기 전에 남기는 결제 시도 기록").isEqualTo(1);
        assertThat(fixtures.countWhere("payment", "status = 'APPROVED'")).as("승인된 결제").isEqualTo(1);
        assertThat(fixtures.orderStatus(orderId)).isEqualTo("PAID");
        assertThat(fixtures.stockOf(product.getId())).isEqualTo(99);
    }

    @Test
    @DisplayName("Idempotency-Key가 없으면 400")
    void missingKey() {
        String token = signup();
        HttpHeaders headers = auth(token);
        ResponseEntity<JsonNode> res = rest.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(body(product.getId(), 1), headers), JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("code").asText()).isEqualTo("I001");
    }

    @Test
    @DisplayName("인가: 다른 사람의 주문은 404, 토큰 없으면 401, 일반 사용자의 관리자 API는 403")
    void authorization() {
        String owner = signup();
        String other = signup();
        long orderId = order(owner, UUID.randomUUID().toString(), product.getId(), 1).getBody().get("id").asLong();

        assertThat(rest.exchange("/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(auth(owner)), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.exchange("/api/orders/" + orderId, HttpMethod.GET, new HttpEntity<>(auth(other)), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rest.getForEntity("/api/orders/" + orderId, JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(rest.exchange("/api/admin/orders", HttpMethod.GET, new HttpEntity<>(auth(owner)), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("에러 응답에는 traceId가 포함되어 로그와 연결된다")
    void traceId() {
        ResponseEntity<JsonNode> res = rest.getForEntity("/api/products/999999", JsonNode.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody().get("traceId").asText()).isEqualTo(res.getHeaders().getFirst("X-Trace-Id"));
    }

    private ResponseEntity<JsonNode> order(String token, String key, Long productId, int qty) {
        HttpHeaders headers = auth(token);
        headers.set("Idempotency-Key", key);
        return rest.exchange("/api/orders", HttpMethod.POST, new HttpEntity<>(body(productId, qty), headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> pay(String token, String key, long orderId) {
        HttpHeaders headers = auth(token);
        headers.set("Idempotency-Key", key);
        return rest.exchange("/api/orders/" + orderId + "/payment", HttpMethod.POST, new HttpEntity<>(headers), JsonNode.class);
    }

    private Map<String, Object> body(Long productId, int qty) {
        return Map.of("items", List.of(Map.of("productId", productId, "quantity", qty)));
    }

    private String signup() {
        String email = "u" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        ResponseEntity<JsonNode> res = rest.postForEntity("/api/auth/signup",
                Map.of("loginId", email.substring(0, email.indexOf("@")), "email", email, "password", "password123!", "name", "tester"), JsonNode.class);
        return res.getBody().get("accessToken").asText();
    }

    private HttpHeaders auth(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
