package com.needsshop.product;

import com.needsshop.product.ProductDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProductCatalogTest {

    @Autowired ProductQueryService service;
    @Autowired ProductRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("delete from order_item");
        jdbc.execute("delete from orders");
        jdbc.execute("delete from product_review");
        jdbc.execute("delete from product");
        repository.saveAll(List.of(
                Product.builder().name("무선A").subCategory(SubCategory.MOUSE_WIRELESS).price(20_000).stock(10).build(),
                Product.builder().name("게이밍B").subCategory(SubCategory.MOUSE_GAMING).price(50_000).stock(0).build(),
                Product.builder().name("할인C").subCategory(SubCategory.MOUSE_WIRELESS).price(10_000).originalPrice(20_000L)
                        .stock(5).build(),
                Product.builder().name("키보드D").subCategory(SubCategory.KEYBOARD_GAMING).price(30_000).originalPrice(40_000L)
                        .stock(5).build(),
                Product.builder().name("키보드E").subCategory(SubCategory.KEYBOARD_GAMING).price(1_000).stock(5).build()));
    }

    @Test
    @DisplayName("카테고리 트리: 소분류별 개수, 대분류 합계, 상품 0개인 소분류도 포함")
    void categoryTree() {
        CategoryTree tree = service.categories();

        assertThat(tree.total()).isEqualTo(5);
        CategoryNode mouse = tree.categories().stream().filter(c -> c.code().equals("MOUSE")).findFirst().orElseThrow();
        assertThat(mouse.label()).isEqualTo("마우스");
        assertThat(mouse.count()).isEqualTo(3);
        assertThat(mouse.children()).extracting(CategoryNode::label).containsExactly("무선", "게이밍");
        CategoryNode monitor = tree.categories().stream().filter(c -> c.code().equals("MONITOR")).findFirst().orElseThrow();
        assertThat(monitor.count()).isZero();
        assertThat(monitor.children()).isNotEmpty();
    }

    @Test
    @DisplayName("필터: 소분류 + 품절 제외 + 가격 상한")
    void filters() {
        List<ProductResponse> result = service.search(new SearchCondition(
                ProductCategory.MOUSE, null, null, 30_000L, true, Sort.PRICE_ASC));
        assertThat(result).extracting(ProductResponse::name).containsExactly("할인C", "무선A");

        List<ProductResponse> gaming = service.search(new SearchCondition(
                null, SubCategory.MOUSE_GAMING, null, null, false, Sort.POPULAR));
        assertThat(gaming).extracting(ProductResponse::name).containsExactly("게이밍B");
    }

    @Test
    @DisplayName("품절 여부와 할인율은 응답에서 계산해서 내려준다 (정가가 없으면 할인율 0)")
    void soldOutAndDiscount() {
        List<ProductResponse> all = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));

        ProductResponse soldOut = all.stream().filter(p -> p.name().equals("게이밍B")).findFirst().orElseThrow();
        ProductResponse discounted = all.stream().filter(p -> p.name().equals("할인C")).findFirst().orElseThrow();
        ProductResponse regular = all.stream().filter(p -> p.name().equals("무선A")).findFirst().orElseThrow();
        assertThat(soldOut.soldOut()).isTrue();
        assertThat(discounted.soldOut()).isFalse();
        assertThat(discounted.discountRate()).isEqualTo(50);
        assertThat(regular.discountRate()).isZero();
    }

    @Test
    @DisplayName("인기순: 판매량(sold_count)이 많은 순서, 같으면 등록 순")
    void popularSort() {
        jdbc.update("update product set sold_count = 7 where name = '키보드E'");
        jdbc.update("update product set sold_count = 3 where name = '무선A'");

        List<ProductResponse> result = service.search(new SearchCondition(null, null, null, null, false, Sort.POPULAR));

        assertThat(result.subList(0, 2)).extracting(ProductResponse::name).containsExactly("키보드E", "무선A");
    }

    @Test
    @DisplayName("키워드 필터: 부분 일치, %는 와일드카드가 아니라 글자로 취급")
    void keyword() {
        assertThat(service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC, "키보드")))
                .extracting(ProductResponse::name).containsExactly("키보드E", "키보드D");
        assertThat(service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC, "%"))).isEmpty();
    }

    @Test
    @DisplayName("가격 정렬: 품절 상품(게이밍B, 5만원)은 가격과 상관없이 맨 뒤로 가고, 나머지는 가격순이다")
    void priceSort() {
        List<ProductResponse> desc = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_DESC));
        assertThat(desc).extracting(ProductResponse::name).containsExactly("키보드D", "무선A", "할인C", "키보드E", "게이밍B");
        assertThat(desc.subList(0, 4)).extracting(ProductResponse::price).isSortedAccordingTo((a, b) -> Long.compare(b, a));

        List<ProductResponse> asc = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));
        assertThat(asc).extracting(ProductResponse::name).containsExactly("키보드E", "할인C", "무선A", "키보드D", "게이밍B");
    }

    @Test
    @DisplayName("인기순에서도 품절은 맨 뒤: 판매량이 가장 많아도 재고 0이면 마지막이고, 품절끼리는 판매량순")
    void soldOutLastInPopularSort() {
        jdbc.update("update product set sold_count = 100 where name = '게이밍B'");   // 품절인데 가장 많이 팔림
        jdbc.update("update product set sold_count = 7 where name = '키보드E'");
        jdbc.update("update product set sold_count = 3 where name = '무선A'");

        List<ProductResponse> result = service.search(new SearchCondition(null, null, null, null, false, Sort.POPULAR));

        assertThat(result).extracting(ProductResponse::name).startsWith("키보드E", "무선A");
        assertThat(result.get(result.size() - 1).name()).isEqualTo("게이밍B");
        assertThat(result.get(result.size() - 1).soldOut()).isTrue();
    }

    @Test
    @DisplayName("품절이 여럿이어도 품절끼리는 원래 정렬 규칙을 따른다")
    void soldOutGroupKeepsOrder() {
        jdbc.update("update product set stock = 0 where name = '무선A'");

        List<ProductResponse> result = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));

        assertThat(result).extracting(ProductResponse::name).containsExactly("키보드E", "할인C", "키보드D", "무선A", "게이밍B");
    }

    @Test
    @DisplayName("단어 검색 결과도 품절을 뒤로 보낸다 (가격순은 그대로 유지)")
    void wordSearchSoldOutLast() {
        jdbc.update("update product set stock = 0 where name = '키보드E'");   // 가장 싼 상품을 품절로

        List<ProductResponse> all = service.findAllWords(new ProductQueryService.WordQuery(null, null, null,
                List.of(), List.of("키보드"), List.of()));
        assertThat(all).extracting(ProductResponse::name).containsExactly("키보드D", "키보드E");

        List<ProductResponse> best = service.findBestWords(new ProductQueryService.WordQuery(null, null, null,
                List.of(), List.of("키보드"), List.of()));
        assertThat(best).extracting(ProductResponse::name).containsExactly("키보드D", "키보드E");
    }

    @Test
    @DisplayName("핫딜 배지: 할인 중이고 재고가 1~10개일 때만. 재고 0, 11개 이상, 할인 없음은 제외")
    void hotDealBadge() {
        // 할인C: 할인 중, 재고 5 -> 핫딜. 키보드D: 할인 중, 재고 5 -> 핫딜. 키보드E: 할인 없음. 게이밍B: 재고 0
        jdbc.update("update product set stock = 10 where name = '키보드D'");   // 경계: 10개는 핫딜
        List<ProductResponse> all = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));

        assertThat(badgesOf(all, "할인C")).containsExactly("HOT_DEAL");
        assertThat(badgesOf(all, "키보드D")).containsExactly("HOT_DEAL");
        assertThat(badgesOf(all, "키보드E")).as("할인 없는 저재고").isEmpty();
        assertThat(badgesOf(all, "무선A")).isEmpty();

        jdbc.update("update product set stock = 11 where name = '키보드D'");   // 경계: 11개는 아님
        jdbc.update("update product set stock = 0 where name = '할인C'");      // 품절은 아님
        List<ProductResponse> after = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));
        assertThat(badgesOf(after, "키보드D")).isEmpty();
        assertThat(badgesOf(after, "할인C")).isEmpty();
    }

    @Test
    @DisplayName("후기가 없으면 평점 0, 있으면 합계/개수를 소수 첫째 자리로 반올림")
    void ratingAverage() {
        jdbc.update("update product set review_count = 3, rating_sum = 13 where name = '무선A'");   // 4.333...
        jdbc.update("update product set review_count = 2, rating_sum = 9 where name = '키보드D'");   // 4.5

        List<ProductResponse> all = service.search(new SearchCondition(null, null, null, null, false, Sort.PRICE_ASC));

        assertThat(all.stream().filter(p -> p.name().equals("무선A")).findFirst().orElseThrow().ratingAvg()).isEqualTo(4.3);
        assertThat(all.stream().filter(p -> p.name().equals("무선A")).findFirst().orElseThrow().reviewCount()).isEqualTo(3);
        assertThat(all.stream().filter(p -> p.name().equals("키보드D")).findFirst().orElseThrow().ratingAvg()).isEqualTo(4.5);
        ProductResponse none = all.stream().filter(p -> p.name().equals("키보드E")).findFirst().orElseThrow();
        assertThat(none.ratingAvg()).isZero();
        assertThat(none.reviewCount()).isZero();
    }

    private static List<String> badgesOf(List<ProductResponse> list, String name) {
        return list.stream().filter(p -> p.name().equals(name)).findFirst().orElseThrow().badges();
    }
}
