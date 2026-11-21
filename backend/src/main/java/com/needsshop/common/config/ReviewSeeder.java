package com.needsshop.common.config;

import com.needsshop.product.Product;
import com.needsshop.product.ProductRepository;
import com.needsshop.product.Review;
import com.needsshop.product.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시드 상품에 샘플 후기를 넣고, 상품의 후기 수·별점 합계(product.review_count, rating_sum)를 같이 채운다.
 * 후기가 하나라도 있으면 아무것도 하지 않으므로(멱등) 몇 번 불러도 같고, 후기 기능이 생기기 전에 만들어 둔 DB에도 한 번 채워진다.
 * 문구와 분포는 {@link ReviewSampleGenerator}가 상품 이름으로 정한 고정 난수로 만든다: 실행할 때마다 같은 후기가 나온다.
 * 전부 샘플 데이터다. 시드가 켜진 곳(needsshop.seed.enabled)에서만 만들어지는 빈이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "needsshop.seed.enabled", havingValue = "true")
public class ReviewSeeder {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    /** @return 새로 넣은 후기 수 (이미 있으면 0) */
    @Transactional
    public int seedIfEmpty() {
        if (reviewRepository.count() > 0) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        int total = 0;
        for (Product product : productRepository.findAll()) {
            List<ReviewSampleGenerator.Sample> samples = ReviewSampleGenerator.generate(product.getName(), product.getSubCategory());
            if (samples.isEmpty()) continue;
            reviewRepository.saveAll(samples.stream()
                    .map(s -> new Review(product.getId(), s.rating(), s.body(), s.nickname(), now.minusMinutes(s.minutesAgo())))
                    .toList());
            product.applyReviewStats(samples.size(), samples.stream().mapToInt(ReviewSampleGenerator.Sample::rating).sum());
            total += samples.size();
        }
        log.info("event=SEED_REVIEWS_DONE reviews={}", total);
        return total;
    }
}
