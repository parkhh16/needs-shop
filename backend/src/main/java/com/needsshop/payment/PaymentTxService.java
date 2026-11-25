package com.needsshop.payment;

import com.needsshop.order.Order;
import com.needsshop.order.OrderRepository;
import com.needsshop.order.OrderService;
import com.needsshop.order.OrderStatus;
import com.needsshop.payment.PgClient.PgResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 결제 흐름에서 DB 트랜잭션 구간만 모아 둔 서비스.
 * PG 호출(최대 수 초)을 트랜잭션 밖으로 빼서, 외부 지연이 DB 커넥션 풀 고갈로 번지지 않게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTxService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    public record Prepared(Long paymentId, Long orderId, String orderNo, String paymentKey, long amount) {
    }

    /** [TX 1] 주문을 PAYING으로 바꾸고, PG 호출 "전에" 결제 시도를 기록한다 */
    @Transactional
    public Prepared prepare(Long userId, Long orderId) {
        Order order = orderService.findMyOrder(userId, orderId);
        order.startPayment(LocalDateTime.now(clock));
        // 상태 변경(@Version)을 결제 기록보다 먼저 DB에 보낸다. 같은 주문에 다른 Idempotency-Key로 결제가 동시에 오면
        // 늦은 쪽은 여기서 낙관적 락 충돌(409)로 끝난다. 미루면 둘 다 같은 결제 키(-P1)를 만들어 INSERT하다가
        // 유니크 제약 위반으로 500이 된다 (돈은 유니크 제약이 지키지만 사용자에게는 서버 오류로 보임)
        orderRepository.saveAndFlush(order);

        // 아래 count + 1로 만드는 결제 키가 겹치지 않는 근거: 결제 행을 INSERT하는 곳은 여기뿐이고, 여기는 반드시 위에서
        // 주문 @Version을 올린다. 그래서 주문 행 락이 "시도 번호 발급"을 줄 세운다. 주문 버전을 올리지 않고 payment를
        // INSERT하는 경로를 새로 만들면 이 전제가 깨진다 (돈은 결제 키 유니크 제약이 지키지만 500이 다시 날 수 있다)
        long attempt = paymentRepository.countByOrderId(orderId) + 1;
        String paymentKey = order.getOrderNo() + "-P" + attempt;
        Payment payment = paymentRepository.save(new Payment(orderId, paymentKey, order.getTotalAmount()));

        log.info("event=PAYMENT_REQUESTED orderNo={} paymentKey={} amount={}",
                order.getOrderNo(), paymentKey, order.getTotalAmount());
        return new Prepared(payment.getId(), orderId, order.getOrderNo(), paymentKey, order.getTotalAmount());
    }

    /** [TX 2] PG 결과를 결제와 주문에 함께 반영한다. 사용자 요청 흐름과 대사 스케줄러가 같이 사용한다 */
    @Transactional
    public PaymentStatus complete(Long paymentId, PgResult result) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        if (payment.isFinished()) {
            return payment.getStatus(); // 이미 다른 경로(요청 흐름과 대사)에서 확정했다
        }
        Order order = orderRepository.findById(payment.getOrderId()).orElseThrow();

        switch (result.outcome()) {
            case APPROVED -> {
                payment.approve(result.transactionId());
                order.markPaid();
                log.info("event=PAYMENT_APPROVED orderNo={} paymentKey={} pgTx={}",
                        order.getOrderNo(), payment.getPaymentKey(), result.transactionId());
            }
            case DECLINED, NOT_FOUND, CANCELLED -> {
                payment.fail(result.reason());
                order.paymentFailed(LocalDateTime.now(clock));
                if (order.getStatus() == OrderStatus.EXPIRED) {
                    orderService.restoreStock(order);
                }
                log.info("event=PAYMENT_FAILED orderNo={} paymentKey={} reason={} orderStatus={}",
                        order.getOrderNo(), payment.getPaymentKey(), result.reason(), order.getStatus());
            }
            case UNKNOWN -> {
                payment.markUnknown(result.reason());
                log.warn("event=PAYMENT_UNKNOWN orderNo={} paymentKey={} reason={}",
                        order.getOrderNo(), payment.getPaymentKey(), result.reason());
            }
        }
        return payment.getStatus();
    }
}
