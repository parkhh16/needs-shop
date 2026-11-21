package com.needsshop.product;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;
import java.util.Map;

public final class ProductDtos {

    private ProductDtos() {
    }

    public enum Sort {POPULAR, PRICE_ASC, PRICE_DESC}

    public record SearchCondition(ProductCategory category, SubCategory sub, Long minPrice,
                                  Long maxPrice, boolean excludeSoldOut, Sort sort, String keyword) {

        public SearchCondition(ProductCategory category, SubCategory sub, Long minPrice, Long maxPrice,
                               boolean excludeSoldOut, Sort sort) {
            this(category, sub, minPrice, maxPrice, excludeSoldOut, sort, null);
        }
    }

    /** 목록용. 상세 설명/스펙처럼 무거운 필드는 빼서 응답 크기를 줄인다 */
    public record ProductResponse(Long id, String name, ProductCategory category, String categoryLabel,
                                  SubCategory subCategory, String subCategoryLabel, long price, Long originalPrice,
                                  int discountRate, int stock, boolean soldOut, int soldCount, String description,
                                  String thumbnail, Integer perUserLimit, double ratingAvg, int reviewCount,
                                  List<String> badges) {

        public static final String HOT_DEAL = "HOT_DEAL";
        public static final String POPULAR = "POPULAR";

        /** HOT_DEAL(할인 중이고 재고가 1~10개)까지만 여기서 계산한다. POPULAR는 최근 주문을 봐야 해서 {@link ProductBadgeService}가 붙인다 */
        public static ProductResponse from(Product p) {
            boolean hotDeal = p.discountRate() > 0 && p.getStock() >= 1 && p.getStock() <= 10;
            return new ProductResponse(p.getId(), p.getName(), p.getCategory(), p.getCategory().getLabel(),
                    p.getSubCategory(), p.getSubCategory().getLabel(), p.getPrice(), p.getOriginalPrice(),
                    p.discountRate(), p.getStock(), p.isSoldOut(), p.getSoldCount(), p.getDescription(), p.thumbnail(),
                    p.getPerUserLimit(), p.ratingAvg(), p.getReviewCount(), hotDeal ? List.of(HOT_DEAL) : List.of());
        }

        public ProductResponse withBadge(String badge) {
            if (badges.contains(badge)) return this;
            List<String> next = new java.util.ArrayList<>(badges);
            next.add(badge);
            return new ProductResponse(id, name, category, categoryLabel, subCategory, subCategoryLabel, price,
                    originalPrice, discountRate, stock, soldOut, soldCount, description, thumbnail, perUserLimit,
                    ratingAvg, reviewCount, List.copyOf(next));
        }
    }

    /** 상세 화면의 후기 요약. 후기가 없어도 distribution의 1~5를 0으로 채워 항상 내려준다 */
    public record ReviewSummary(double average, int count, Map<Integer, Integer> distribution) {
    }

    public record ReviewItem(int rating, String body, String nickname, String date) {
    }

    public record ReviewPage(List<ReviewItem> items, int page, int size, long total, boolean hasNext) {
    }

    /** 목록 필드를 평탄화(@JsonUnwrapped)해서 기존 GET /api/products/{id} 응답 형태(stock 등 최상위 필드)를 유지한다 */
    public record ProductDetailResponse(@JsonUnwrapped ProductResponse summary, String detail, Map<String, String> specs,
                                        List<String> images, List<ProductResponse> related, ReviewSummary reviewSummary) {
    }

    public record CategoryNode(String code, String label, long count, List<CategoryNode> children) {
    }

    public record CategoryTree(long total, List<CategoryNode> categories) {
    }
}
