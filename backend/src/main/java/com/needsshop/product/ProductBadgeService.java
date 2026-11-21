package com.needsshop.product;

import com.needsshop.order.OrderRepository;
import com.needsshop.product.ProductDtos.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인기(POPULAR) 배지: 최근 7일 동안 결제 완료된 수량이 많은 상위 3개 상품 중, 수량이 5개 이상인 것.
 * 목록·상세 요청마다 주문을 집계하지 않도록 결과를 짧게(기본 30초) 들고 있는다.
 *
 * <p>서버 2대일 때: 서버마다 따로 들고 있어서 배지가 서로 최대 30초 어긋날 수 있다. 화면 표시용 파생 값이라
 * 재고·돈에는 영향이 없고, 틀려도 다음 갱신 때 맞춰지므로 서버 간 전파는 하지 않는다.
 *
 * <p>집계는 호출한 쪽(목록 조회) 트랜잭션과 따로 도는 새 읽기 전용 트랜잭션에서 한다. 같은 트랜잭션에 참여하면 집계가 실패했을 때
 * 바깥 트랜잭션이 rollback-only가 되어, 예외를 삼켜도 목록 응답이 500이 된다. 실패하면 직전 값으로 {@link #RETRY_MS} 뒤에 다시 시도한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductBadgeService {

    static final int DAYS = 7;
    static final int TOP = 3;
    static final long MIN_QUANTITY = 5;
    static final long RETRY_MS = 5_000;

    private final OrderRepository orderRepository;
    private final Clock clock;
    private final PlatformTransactionManager transactionManager;

    @Value("${needsshop.popular.cache-ttl:30s}")
    private Duration cacheTtl;

    private volatile Cached cached;

    private record Cached(Set<Long> ids, long validUntilMillis) {
    }

    public List<ProductResponse> apply(List<ProductResponse> products) {
        if (products.isEmpty()) return products;
        Set<Long> popular = popularIds();
        if (popular.isEmpty()) return products;
        return products.stream().map(p -> popular.contains(p.id()) ? p.withBadge(ProductResponse.POPULAR) : p).toList();
    }

    public ProductResponse apply(ProductResponse product) {
        return popularIds().contains(product.id()) ? product.withBadge(ProductResponse.POPULAR) : product;
    }

    public Set<Long> popularIds() {
        Cached c = cached;
        if (c != null && clock.millis() < c.validUntilMillis()) return c.ids();
        return refresh();
    }

    /** 캐시가 끝난 순간 몰린 요청이 모두 집계하지 않도록 한 번에 하나만 갱신한다 */
    private synchronized Set<Long> refresh() {
        long now = clock.millis();
        Cached c = cached;
        if (c != null && now < c.validUntilMillis()) return c.ids();
        try {
            Set<Long> ids = load();
            cached = new Cached(ids, now + cacheTtl.toMillis());
            return ids;
        } catch (RuntimeException e) {
            // 배지는 부가 정보다. 집계가 실패해도 상품 목록은 배지 없이(또는 직전 값으로) 내려주고, 잠시 뒤 다시 시도한다
            log.warn("event=POPULAR_BADGE_FAILED cause={}", e.toString());
            Set<Long> previous = c != null ? c.ids() : Set.of();
            cached = new Cached(previous, now + Math.min(RETRY_MS, cacheTtl.toMillis()));
            return previous;
        }
    }

    private Set<Long> load() {
        LocalDateTime since = LocalDateTime.now(clock).minusDays(DAYS);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setReadOnly(true);
        return tx.execute(status -> orderRepository.findTopPaidQuantitySince(since, MIN_QUANTITY, PageRequest.of(0, TOP)).stream()
                .map(OrderRepository.ProductHeld::getProductId)
                .collect(Collectors.toUnmodifiableSet()));
    }
}
