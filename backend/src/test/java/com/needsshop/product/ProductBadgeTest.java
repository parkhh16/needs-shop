package com.needsshop.product;

import com.needsshop.order.Order;
import com.needsshop.order.OrderRepository;
import com.needsshop.product.ProductDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 인기(POPULAR) 배지: 최근 7일 결제 완료(PAID) 수량 상위 3개, 5개 이상.
 * (테스트 프로필은 집계 캐시를 꺼 둔다: needsshop.popular.cache-ttl=0s)
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductBadgeTest {

    @Autowired ProductQueryService service;
    @Autowired ProductRepository repository;
    @Autowired OrderRepository orderRepository;
    @Autowired JdbcTemplate jdbc;

    Product a, b, c, d, e, f;

    @BeforeEach
    void setUp() {
        jdbc.execute("delete from payment");
        jdbc.execute("delete from order_item");
        jdbc.execute("delete from orders");
        jdbc.execute("delete from product_review");
        jdbc.execute("delete from product");
        a = save("마우스A", 10_000, 50);
        b = save("마우스B", 20_000, 50);
        c = save("마우스C", 30_000, 50);
        d = save("마우스D", 40_000, 50);
        e = save("마우스E", 50_000, 50);
        f = save("마우스F", 60_000, 50);
    }

    private Product save(String name, long price, int stock) {
        return repository.save(Product.builder().name(name).subCategory(SubCategory.MOUSE_WIRELESS).price(price).stock(stock).build());
    }

    /** 주문 하나 (상태: PAID 또는 결제 대기). createdAt은 daysAgo일 전으로 옮긴다 */
    private void order(Product p, int quantity, boolean paid, int daysAgo) {
        Order order = Order.create(1L, LocalDateTime.now().plusMinutes(10));
        order.addItem(p.getId(), p.getName(), p.getPrice(), quantity);
        if (paid) {
            order.startPayment(LocalDateTime.now());
            order.markPaid();
        }
        orderRepository.saveAndFlush(order);
        LocalDateTime at = LocalDateTime.now().minusDays(daysAgo);
        jdbc.update("update orders set created_at = ?, expires_at = ? where id = ?", at, at.plusMinutes(10), order.getId());
    }

    private List<ProductResponse> all() {
        return service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));
    }

    private static List<String> badges(List<ProductResponse> list, String name) {
        return list.stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow().badges();
    }

    @Test
    @DisplayName("최근 7일 PAID 수량 상위 3개(5개 이상)만 인기. 4위는 5개 이상이어도 제외")
    void top3WithMinimumQuantity() {
        order(a, 10, true, 1);
        order(b, 8, true, 2);
        order(c, 6, true, 3);
        order(d, 5, true, 4);    // 4위: 수량은 충분하지만 상위 3개가 아님
        order(e, 4, true, 1);    // 5개 미만

        List<ProductResponse> list = all();

        assertThat(badges(list, "마우스A")).containsExactly("POPULAR");
        assertThat(badges(list, "마우스B")).containsExactly("POPULAR");
        assertThat(badges(list, "마우스C")).containsExactly("POPULAR");
        assertThat(badges(list, "마우스D")).isEmpty();
        assertThat(badges(list, "마우스E")).isEmpty();
        assertThat(badges(list, "마우스F")).isEmpty();
    }

    @Test
    @DisplayName("한 상품의 여러 주문은 합쳐서 센다 (3 + 3 = 6)")
    void sumsAcrossOrders() {
        order(a, 3, true, 1);
        order(a, 3, true, 2);

        assertThat(badges(all(), "마우스A")).containsExactly("POPULAR");
    }

    @Test
    @DisplayName("7일이 지난 주문과 결제 전(PENDING) 주문은 세지 않는다")
    void onlyRecentPaid() {
        order(a, 100, true, 8);     // 8일 전: 기간 밖
        order(b, 100, false, 1);    // 결제 대기: 아직 팔린 게 아님
        order(c, 5, true, 6);       // 6일 전: 기간 안

        List<ProductResponse> list = all();

        assertThat(badges(list, "마우스A")).isEmpty();
        assertThat(badges(list, "마우스B")).isEmpty();
        assertThat(badges(list, "마우스C")).containsExactly("POPULAR");
    }

    @Test
    @DisplayName("핫딜과 인기를 함께 받을 수 있고, 상세(본 상품과 관련 상품)에도 붙는다")
    void bothBadgesAndDetail() {
        jdbc.update("update product set original_price = 12000, stock = 3 where id = ?", a.getId());   // 핫딜
        order(a, 7, true, 1);
        order(b, 6, true, 1);

        List<ProductResponse> list = all();
        assertThat(badges(list, "마우스A")).containsExactlyInAnyOrder("HOT_DEAL", "POPULAR");
        assertThat(badges(list, "마우스B")).containsExactly("POPULAR");

        ProductDetailResponse detail = service.detail(a.getId());
        assertThat(detail.summary().badges()).containsExactlyInAnyOrder("HOT_DEAL", "POPULAR");
        assertThat(detail.related()).extracting(ProductResponse::name).contains("마우스B");
        assertThat(detail.related().stream().filter(p -> p.name().equals("마우스B")).findFirst().orElseThrow().badges())
                .containsExactly("POPULAR");
    }

    @Test
    @DisplayName("주문이 없으면 배지가 없고 목록은 정상이다")
    void noOrders() {
        assertThat(all()).hasSize(6).allSatisfy(p -> assertThat(p.badges()).isEmpty());
    }
}
