package com.needsshop.product;

import com.needsshop.common.error.BusinessException;
import com.needsshop.common.error.ErrorCode;
import com.needsshop.product.ProductDtos.ReviewItem;
import com.needsshop.product.ProductDtos.ReviewPage;
import com.needsshop.product.ProductDtos.ReviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/** 후기 읽기 전용 조회 (작성 기능은 없다) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewQueryService {

    static final int MAX_SIZE = 20;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    /** 별점 개수 분포와 평균. 후기가 없어도 1~5를 0으로 채운다 */
    public ReviewSummary summary(Long productId) {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int star = 1; star <= 5; star++) distribution.put(star, 0);
        reviewRepository.countByRating(productId).forEach(c -> distribution.put(c.getRating(), (int) c.getCount()));
        int count = distribution.values().stream().mapToInt(Integer::intValue).sum();
        int sum = distribution.entrySet().stream().mapToInt(e -> e.getKey() * e.getValue()).sum();
        double average = count == 0 ? 0 : Math.round(sum * 10.0 / count) / 10.0;
        return new ReviewSummary(average, count, distribution);
    }

    /** 최신순 후기 목록. size는 1~20으로 맞춘다. 없거나 판매 중지된 상품은 PRODUCT_NOT_FOUND */
    public ReviewPage list(Long productId, int page, int size) {
        productRepository.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Page<Review> result = reviewRepository.findByProductIdOrderByCreatedAtDescIdDesc(productId,
                PageRequest.of(safePage, safeSize));
        return new ReviewPage(result.getContent().stream()
                .map(r -> new ReviewItem(r.getRating(), r.getBody(), r.getNickname(), r.getCreatedAt().toLocalDate().toString()))
                .toList(), safePage, safeSize, result.getTotalElements(), result.hasNext());
    }
}
