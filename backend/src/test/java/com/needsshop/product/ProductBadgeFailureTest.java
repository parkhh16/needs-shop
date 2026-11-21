package com.needsshop.product;

import com.needsshop.order.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** 인기 배지 집계가 실패해도 목록은 막히지 않고, 실패 중에는 매 요청이 아니라 잠시 간격을 두고 다시 시도한다 */
class ProductBadgeFailureTest {

    /** 테스트가 시간을 직접 움직이는 시계 */
    static class MutableClock extends Clock {
        final AtomicLong millis = new AtomicLong(1_700_000_000_000L);

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
    }

    @Test
    @DisplayName("집계가 실패하면 예외 없이 빈 값(또는 직전 값)을 돌려주고, 5초 안에는 다시 집계하지 않는다")
    void failureDoesNotBreakAndRetriesLater() {
        OrderRepository orders = mock(OrderRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(orders.findTopPaidQuantitySince(any(), anyLong(), any())).thenThrow(new IllegalStateException("db down"));
        MutableClock clock = new MutableClock();
        ProductBadgeService service = new ProductBadgeService(orders, clock, tm);
        ReflectionTestUtils.setField(service, "cacheTtl", Duration.ofSeconds(30));

        assertThat(service.popularIds()).isEqualTo(Set.of());
        assertThat(service.popularIds()).isEqualTo(Set.of());
        verify(orders, times(1)).findTopPaidQuantitySince(any(), anyLong(), any());

        clock.millis.addAndGet(5_001);
        assertThat(service.popularIds()).isEqualTo(Set.of());
        verify(orders, times(2)).findTopPaidQuantitySince(any(), anyLong(), any());
    }

    @Test
    @DisplayName("성공한 값은 TTL 동안 재사용하고, 그 뒤 실패하면 직전 값을 유지한다")
    void keepsPreviousValueOnLaterFailure() {
        OrderRepository orders = mock(OrderRepository.class);
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        OrderRepository.ProductHeld held = mock(OrderRepository.ProductHeld.class);
        when(held.getProductId()).thenReturn(7L);
        when(orders.findTopPaidQuantitySince(any(), anyLong(), any()))
                .thenReturn(List.of(held))
                .thenThrow(new IllegalStateException("db down"));
        MutableClock clock = new MutableClock();
        ProductBadgeService service = new ProductBadgeService(orders, clock, tm);
        ReflectionTestUtils.setField(service, "cacheTtl", Duration.ofSeconds(30));

        assertThat(service.popularIds()).containsExactly(7L);
        assertThat(service.popularIds()).containsExactly(7L);
        verify(orders, times(1)).findTopPaidQuantitySince(any(), anyLong(), any());

        clock.millis.addAndGet(31_000);
        assertThat(service.popularIds()).containsExactly(7L);
    }
}
