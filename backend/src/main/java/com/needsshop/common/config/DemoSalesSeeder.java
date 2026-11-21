package com.needsshop.common.config;

import com.needsshop.order.Order;
import com.needsshop.order.OrderRepository;
import com.needsshop.payment.Payment;
import com.needsshop.payment.PaymentRepository;
import com.needsshop.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 데모 판매 기록: 목록의 "인기" 배지(최근 7일 결제 수량 상위 3개, 5개 이상)가 보이도록, 최근 며칠 안에 결제 완료된 주문을 만든다.
 * 실제 손님의 주문이 아닌 샘플 데이터이고, 데모 회원(user1, user2)이 있을 때만 {@link DataInitializer}가 처음 시드할 때 한 번 부른다
 * (운영은 데모 회원이 없어서 만들어지지 않는다).
 *
 * <p>정합성: 주문(PAID) - 승인된 결제(금액 = 주문 금액) - 재고 차감(stock, sold_count)을 실제 결제 흐름과 같은 모양으로 맞춘다.
 * 그래서 정합성 리포트(PaymentRepository#findMismatches)는 계속 0건이다. 엔티티 상태 전이(결제 시작, 승인)를 그대로 거치고,
 * 만든 시각만 JDBC로 최근 며칠 전으로 옮긴다 (BaseTimeEntity는 저장 시점으로 고정하기 때문). 운영 코드 경로(OrderService 등)는 건드리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "needsshop.seed.enabled", havingValue = "true")
public class DemoSalesSeeder {

    /** 상품 이름 -> 주문별 수량. 합계 17, 12, 9, 7: 상위 3개만 인기 배지를 받고, 4번째(7개)는 5개 이상이어도 배지가 없다 */
    private static final Map<String, int[]> SALES = Map.of(
            "저소음 무선 마우스", new int[]{5, 7, 5},
            "대형 장패드", new int[]{4, 8},
            "무선 이어폰", new int[]{3, 6},
            "무선 버티컬 마우스", new int[]{2, 5});

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    /**
     * @param products 같은 트랜잭션에서 저장한 시드 상품 (재고를 엔티티로 차감한다)
     * @return 만든 주문 수
     */
    @Transactional
    public int seed(List<Product> products, List<Long> buyerIds) {
        if (buyerIds.isEmpty()) return 0;
        LocalDateTime now = LocalDateTime.now(clock);
        int orders = 0;
        // Map.of는 순서가 없어서 상품 목록 순서(시드 순서)로 돌려 항상 같은 순서로 만든다
        for (Product product : products) {
            int[] quantities = SALES.get(product.getName());
            if (quantities == null) continue;
            for (int quantity : quantities) {
                // 1~5일 전 (7일 기준선 안쪽). 주문마다 시각이 다르게
                LocalDateTime at = now.minusDays(1 + orders % 5).minusHours(3 + orders);
                createPaidOrder(buyerIds.get(orders % buyerIds.size()), product, quantity, at, now);
                orders++;
            }
        }
        log.info("event=SEED_DEMO_SALES orders={}", orders);
        return orders;
    }

    private void createPaidOrder(Long userId, Product product, int quantity, LocalDateTime at, LocalDateTime now) {
        product.decreaseStock(quantity);   // stock - qty, sold_count + qty (실제 차감과 같은 규칙)

        Order order = Order.create(userId, now.plusMinutes(10));
        order.addItem(product.getId(), product.getName(), product.getPrice(), quantity);
        order.startPayment(now);
        order.markPaid();
        orderRepository.saveAndFlush(order);

        Payment payment = new Payment(order.getId(), "SEED-" + order.getOrderNo(), order.getTotalAmount());
        payment.approve("PG-SEED-" + order.getId());
        paymentRepository.saveAndFlush(payment);

        // 만든 시각을 과거로. 만료 시각도 만든 시각 + 10분으로 같이 옮긴다 (인기 집계의 범위 조건과 모순이 없게)
        jdbc.update("update orders set created_at = ?, updated_at = ?, expires_at = ? where id = ?",
                at, at, at.plusMinutes(10), order.getId());
        jdbc.update("update payment set created_at = ?, updated_at = ? where id = ?", at, at, payment.getId());
    }
}
