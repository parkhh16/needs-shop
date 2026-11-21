package com.needsshop.common.config;

import com.needsshop.payment.PaymentRepository;
import com.needsshop.product.Product;
import com.needsshop.product.ProductDtos.*;
import com.needsshop.product.ProductQueryService;
import com.needsshop.product.ProductRepository;
import com.needsshop.product.Review;
import com.needsshop.product.ReviewRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시드(샘플 후기, 핫딜 재고, 인기 배지용 데모 판매)를 켠 상태에서 확인한다.
 * 후기는 결정적(같은 입력 -> 같은 출력)이어야 하고, 분포는 "전부 4.8" 같은 가짜처럼 보이지 않는 모양이어야 한다.
 */
@SpringBootTest(properties = "needsshop.seed.enabled=true")
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReviewSeedTest {

    @Autowired ProductRepository products;
    @Autowired ReviewRepository reviews;
    @Autowired ReviewSeeder seeder;
    @Autowired ProductQueryService queryService;
    @Autowired PaymentRepository payments;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataInitializer initializer;

    /**
     * MySQL 테스트는 컨텍스트끼리 스키마를 공유해서, 다른 테스트가 남긴 데이터가 있으면 시작 시점의 시드가 건너뛰어진다.
     * 그래서 이 클래스는 데이터를 모두 비우고 시드를 처음부터 다시 돌려서 시작하고, 끝나면 다시 비운다 (다른 테스트에 시드 계정·상품을 남기지 않는다).
     */
    @BeforeAll
    void seedFromScratch() {
        wipe();
        initializer.run(null);
    }

    @AfterAll
    void wipe() {
        for (String table : List.of("payment", "order_item", "orders", "product_review", "activity_log", "product", "users")) {
            jdbc.execute("delete from " + table);
        }
    }

    private List<Product> seeded() {
        return products.findAll();
    }

    @Test
    @DisplayName("결정적: 같은 상품은 두 번 만들어도 같은 후기이고, DB에 들어간 후기도 그 결과와 같다")
    void deterministic() {
        for (Product p : seeded()) {
            List<ReviewSampleGenerator.Sample> first = ReviewSampleGenerator.generate(p.getName(), p.getSubCategory());
            List<ReviewSampleGenerator.Sample> second = ReviewSampleGenerator.generate(p.getName(), p.getSubCategory());
            assertThat(second).as(p.getName()).isEqualTo(first);

            List<Review> stored = reviews.findAll().stream()
                    .filter(r -> r.getProductId().equals(p.getId()))
                    .sorted(Comparator.comparing(Review::getId))
                    .toList();
            assertThat(stored).as(p.getName() + " 개수").hasSameSizeAs(first);
            for (int i = 0; i < first.size(); i++) {
                assertThat(stored.get(i).getRating()).as(p.getName() + " #" + i).isEqualTo(first.get(i).rating());
                assertThat(stored.get(i).getBody()).isEqualTo(first.get(i).body());
                assertThat(stored.get(i).getNickname()).isEqualTo(first.get(i).nickname());
            }
        }
    }

    @Test
    @DisplayName("별점 분포는 J자형: 5점 > 4점 > 1점 > 2점, 1점 > 3점, 1점은 전체의 3~8%, 전체 평균은 4.2~4.6")
    void jShapedDistribution() {
        int[] total = new int[5];
        for (Product p : seeded()) {
            int[] d = ReviewSampleGenerator.distribution(ReviewSampleGenerator.generate(p.getName(), p.getSubCategory()));
            for (int i = 0; i < 5; i++) total[i] += d[i];
        }
        int all = Arrays.stream(total).sum();
        double mean = (total[0] + 2.0 * total[1] + 3.0 * total[2] + 4.0 * total[3] + 5.0 * total[4]) / all;

        assertThat(all).as("전체 후기 수").isBetween(400, 900);
        assertThat(total[4]).as("5점").isGreaterThan(total[3]);
        assertThat(total[3]).as("4점").isGreaterThan(total[0]);
        assertThat(total[0]).as("1점이 2점보다 많다").isGreaterThan(total[1]);
        assertThat(total[0]).as("1점이 3점보다 많다").isGreaterThan(total[2]);
        assertThat(total[0] * 100.0 / all).as("1점 비율(%)").isBetween(3.0, 8.0);
        assertThat(mean).as("전체 평균").isBetween(4.2, 4.6);
    }

    @Test
    @DisplayName("상품별 평균: 대부분 4.0~4.7, 4.8 이상은 소수, 5.0은 후기 3~5개인 상품만, 후기 없는 상품 3~4개, 후기 수는 0 또는 3~60")
    void perProductAverages() {
        List<Product> all = seeded();
        List<Product> reviewed = all.stream().filter(p -> p.getReviewCount() > 0).toList();
        List<Product> none = all.stream().filter(p -> p.getReviewCount() == 0).toList();

        assertThat(none.size()).as("후기 없는 상품").isBetween(3, 4);
        assertThat(reviewed).allSatisfy(p -> assertThat(p.getReviewCount()).as(p.getName()).isBetween(3, 60));

        long usual = reviewed.stream().filter(p -> p.ratingAvg() >= 4.0 && p.ratingAvg() <= 4.7).count();
        assertThat(usual).as("4.0~4.7 상품").isGreaterThanOrEqualTo((long) Math.ceil(reviewed.size() * 0.7));
        List<Product> high = reviewed.stream().filter(p -> p.ratingAvg() >= 4.8).toList();
        assertThat(high.size()).as("4.8 이상 상품").isBetween(1, 4);
        assertThat(reviewed.stream().filter(p -> p.ratingAvg() < 4.0).count()).as("3점대 후반 상품이 일부 있다").isBetween(1L, 4L);
        reviewed.stream().filter(p -> p.ratingAvg() == 5.0)
                .forEach(p -> assertThat(p.getReviewCount()).as(p.getName() + "의 5.0은 후기가 적을 때만").isBetween(3, 5));
        assertThat(reviewed.stream().filter(p -> p.getReviewCount() >= 40).map(Product::getPrice).max(Long::compare).orElse(0L))
                .as("후기가 많은 상품은 값싼 인기 소모품").isLessThanOrEqualTo(40_000L);
    }

    @Test
    @DisplayName("본문 없는 별점 후기는 전체의 15~25%, 같은 상품 안에서 본문이 겹치지 않고, 길이·닉네임·작성일 제약을 지킨다")
    void bodiesAndShape() {
        List<Review> all = reviews.findAll();
        long bodyless = all.stream().filter(r -> r.getBody() == null).count();
        assertThat(bodyless * 100.0 / all.size()).as("본문 없는 후기(%)").isBetween(15.0, 25.0);

        Map<Long, List<String>> bodiesByProduct = all.stream().filter(r -> r.getBody() != null)
                .collect(Collectors.groupingBy(Review::getProductId, Collectors.mapping(Review::getBody, Collectors.toList())));
        bodiesByProduct.forEach((productId, bodies) ->
                assertThat(bodies).as("상품 " + productId + " 본문 중복").doesNotHaveDuplicates());

        LocalDateTime now = LocalDateTime.now();
        assertThat(all).allSatisfy(r -> {
            assertThat(r.getRating()).isBetween(1, 5);
            assertThat(r.getBody() == null || (!r.getBody().isBlank() && r.getBody().length() <= 1000)).isTrue();
            assertThat(r.getNickname()).isNotBlank().hasSizeLessThanOrEqualTo(30);
            assertThat(r.getCreatedAt()).isBefore(now).isAfter(now.minusMonths(6).minusDays(1));
        });
        assertThat(all.stream().map(Review::getNickname).distinct().count()).as("닉네임이 다양하다").isGreaterThan(100);
        // 1~2점 후기는 본문이 있고(불만을 적었다), 광고 같은 반복 문구가 아니다
        assertThat(all.stream().filter(r -> r.getRating() <= 2)).allSatisfy(r -> assertThat(r.getBody()).isNotBlank());
        assertThat(all.stream().map(Review::getBody).filter(Objects::nonNull).filter(b -> b.contains("최고예요")).count()).isZero();
    }

    @Test
    @DisplayName("상품의 review_count, rating_sum은 후기 테이블 집계와 정확히 같다")
    void statsMatchReviewTable() {
        Map<Long, int[]> actual = new HashMap<>();
        jdbc.query("select product_id, count(*), sum(rating) from product_review group by product_id", rs -> {
            actual.put(rs.getLong(1), new int[]{rs.getInt(2), rs.getInt(3)});
        });
        for (Product p : seeded()) {
            int[] expected = actual.getOrDefault(p.getId(), new int[]{0, 0});
            assertThat(p.getReviewCount()).as(p.getName() + " review_count").isEqualTo(expected[0]);
            assertThat(p.getRatingSum()).as(p.getName() + " rating_sum").isEqualTo(expected[1]);
        }
    }

    @Test
    @DisplayName("멱등: 후기가 이미 있으면 다시 불러도 아무것도 넣지 않는다")
    void idempotent() {
        long before = reviews.count();
        int sumBefore = seeded().stream().mapToInt(Product::getRatingSum).sum();

        assertThat(seeder.seedIfEmpty()).isZero();

        assertThat(reviews.count()).isEqualTo(before);
        assertThat(seeded().stream().mapToInt(Product::getRatingSum).sum()).isEqualTo(sumBefore);
    }

    @Test
    @DisplayName("상세: reviewSummary의 분포는 후기 테이블과 같고 평균이 카드의 평점과 같다")
    void detailSummaryMatchesCard() {
        for (Product p : seeded()) {
            ProductDetailResponse detail = queryService.detail(p.getId());
            assertThat(detail.reviewSummary().count()).as(p.getName()).isEqualTo(p.getReviewCount());
            assertThat(detail.reviewSummary().average()).as(p.getName()).isEqualTo(p.ratingAvg());
            assertThat(detail.reviewSummary().distribution().keySet()).containsExactly(1, 2, 3, 4, 5);
            assertThat(detail.summary().ratingAvg()).isEqualTo(p.ratingAvg());
        }
    }

    @Test
    @DisplayName("핫딜: 재고 10개 이하 상품 중 일부만 할인이 있고, 할인+저재고인 상품만 HOT_DEAL (재고 0은 제외)")
    void hotDeals() {
        List<ProductResponse> list = queryService.search(new SearchCondition(null, null, null, null, false, Sort.POPULAR));

        List<ProductResponse> lowStock = list.stream().filter(p -> p.stock() >= 1 && p.stock() <= 10).toList();
        List<String> hot = list.stream().filter(p -> p.badges().contains("HOT_DEAL")).map(ProductResponse::name).toList();

        assertThat(lowStock.size()).as("저재고 상품").isGreaterThanOrEqualTo(4);
        assertThat(hot).as("핫딜").containsExactlyInAnyOrder("8K 폴링 게이밍 마우스", "7.1채널 게이밍 헤드셋", "PBT 키캡 세트");
        assertThat(hot.size() * 2.0 / lowStock.size()).as("저재고 중 할인 비율이 절반 안팎").isBetween(0.8, 1.2);
        assertThat(list.stream().filter(p -> p.soldOut() && p.badges().contains("HOT_DEAL"))).isEmpty();
    }

    @Test
    @DisplayName("인기 배지: 데모 판매 상위 3개만 받고(4번째는 수량이 5개 이상이어도 제외), 품절 상품은 목록 맨 뒤")
    void popularAndSoldOutLast() {
        List<ProductResponse> list = queryService.search(new SearchCondition(null, null, null, null, false, Sort.POPULAR));

        assertThat(list.stream().filter(p -> p.badges().contains("POPULAR")).map(ProductResponse::name))
                .containsExactlyInAnyOrder("저소음 무선 마우스", "대형 장패드", "무선 이어폰");
        // 인기순 맨 앞은 가장 많이 팔린 두 상품
        assertThat(list.get(0).name()).isEqualTo("저소음 무선 마우스");
        assertThat(list.get(list.size() - 1).name()).isEqualTo("32인치 4K 모니터");
        assertThat(list.get(list.size() - 1).soldOut()).isTrue();
    }

    @Test
    @DisplayName("데모 판매 정합성: PAID 주문 - 승인 결제 - 재고(sold_count) 불일치 0건, 주문은 모두 최근 7일 안")
    void demoSalesAreConsistent() {
        assertThat(payments.findMismatches()).as("정합성 리포트").isEmpty();

        Integer paid = jdbc.queryForObject("select count(*) from orders where status = 'PAID'", Integer.class);
        Integer approved = jdbc.queryForObject("select count(*) from payment where status = 'APPROVED'", Integer.class);
        assertThat(paid).isEqualTo(9).isEqualTo(approved);

        Integer recent = jdbc.queryForObject("select count(*) from orders where status = 'PAID' and created_at >= ?",
                Integer.class, LocalDateTime.now().minusDays(7));
        assertThat(recent).as("모든 데모 주문이 최근 7일 안").isEqualTo(9);

        // 판매 수량 = sold_count = 처음 재고에서 줄어든 양
        Map<String, Integer> soldByName = new HashMap<>();
        jdbc.query("select i.product_name, sum(i.quantity) from order_item i join orders o on o.id = i.order_id " +
                "where o.status = 'PAID' group by i.product_name", rs -> {
            soldByName.put(rs.getString(1), rs.getInt(2));
        });
        assertThat(soldByName).containsEntry("저소음 무선 마우스", 17).containsEntry("대형 장패드", 12)
                .containsEntry("무선 이어폰", 9).containsEntry("무선 버티컬 마우스", 7);
        for (Product p : seeded()) {
            assertThat(p.getSoldCount()).as(p.getName() + " sold_count").isEqualTo(soldByName.getOrDefault(p.getName(), 0));
        }
        Map<String, Integer> initialStock = Map.of("저소음 무선 마우스", 180, "대형 장패드", 1000, "무선 이어폰", 300, "무선 버티컬 마우스", 400);
        for (Product p : seeded()) {
            if (initialStock.containsKey(p.getName())) {
                assertThat(p.getStock()).as(p.getName() + " 재고").isEqualTo(initialStock.get(p.getName()) - p.getSoldCount());
            }
        }
    }
}
