# 따닥 클릭과 재전송으로 주문이 두 번 생기는 문제

## 문제

사용자가 주문 버튼을 빠르게 두 번 누르거나, 모바일 네트워크가 끊겨서 클라이언트가 같은 요청을 다시 보내면 주문과 결제가 두 번 생깁니다. 재고가 한정된 상품에서는 다른 사람이 살 수 있던 재고까지 한 사람이 가져가게 됩니다.

## 해결: `Idempotency-Key` + DB 유니크 제약

주문·결제 API는 요청마다 `Idempotency-Key` 헤더를 받습니다. 클라이언트는 "버튼 한 번 누름 = 키 하나"로 만들고, 재전송할 때는 같은 키를 씁니다.

```
1. INSERT idempotency_record(user_id, key, request_hash, IN_PROGRESS)   ← UNIQUE(user_id, key)
   └ 이미 있으면
       - 요청 내용(해시)이 다름 → 422 (같은 키로 다른 요청을 보낸 클라이언트 버그)
       - 아직 처리 중         → 409
       - 처리 완료            → 저장해 둔 응답을 그대로 돌려줌 (Idempotent-Replayed: true)
2. INSERT에 성공한 요청만 실제로 주문 처리 → COMPLETED + 응답 본문 저장
3. 처리 중 실패하면 레코드를 지움 → 같은 키로 다시 시도할 수 있게
```

### 왜 DB 유니크 제약인가

`synchronized`나 애플리케이션 메모리의 락은 서버가 두 대만 돼도 소용이 없습니다. 유니크 제약은 서버 대수와 상관없이 DB가 보장합니다. "먼저 조회해서 없으면 저장"하는 방식은 조회와 저장 사이에 다른 요청이 끼어들 수 있어서, 동시 요청 중 하나만 통과시키는 역할은 INSERT와 유니크 제약에 맡겼습니다.

### 왜 요청 해시를 저장하나

같은 키로 수량 1개 주문을 보냈다가 수량 2개로 다시 보내는 경우, 조용히 첫 응답을 돌려주면 클라이언트는 2개를 샀다고 착각합니다. 요청 본문 해시를 저장해 두고 다르면 422로 알립니다.

## 결과

- 같은 키로 요청 20개를 동시에 보내도 **주문 1건, 재고 1개 차감**
- 두 번째부터는 첫 응답이 그대로 재생되고, 활동 로그에 "멱등성 재생"으로 따로 남습니다.
- `OrderApiTest.doubleClick`이 같은 시나리오를 자동으로 검증합니다 (`./gradlew test --tests "*OrderApiTest"`).

결제는 한 번 더 막힙니다. 결제 시작 시 주문 상태가 `PENDING_PAYMENT → PAYING`으로 바뀌는데, 상태 머신이 `PAYING → PAYING` 전이를 허용하지 않아서 같은 주문을 동시에 두 번 결제할 수 없습니다.

## 키가 다르면: 같은 주문에 결제가 동시에 여러 번 올 때 (2026-11-25)

### 문제

멱등 키는 **같은 키**로 다시 온 요청만 알아봅니다. 내 활동 화면의 "결제하기" 버튼은 `useState`로만 막고 클릭마다 새 키를 만들어서(`MyActivityPage.tsx`), 같은 틱에 두 번 눌리면 **다른 키 두 개로 같은 주문을 결제**하는 요청이 나갈 수 있습니다. 이 경우를 막는 테스트가 없었습니다.

테스트를 만들어 보니(같은 주문, 서로 다른 키, 결제 10건 동시) 승인은 1건이었지만 나머지가 **500 서버 오류**로 돌아갔습니다.

### 원인

결제 시작 트랜잭션(`PaymentTxService.prepare`)이 주문을 PAYING으로 바꾸고, 결제 시도 기록을 INSERT합니다. 결제 키는 `주문번호-P{시도 횟수+1}`입니다.

```
요청 A: 주문 읽음(PENDING_PAYMENT, version 3) → PAYING으로 변경(아직 메모리) → 시도 0건 → 결제 키 -P1 INSERT → 커밋
요청 B: 주문 읽음(PENDING_PAYMENT, version 3) → PAYING으로 변경(아직 메모리) → 시도 0건 → 결제 키 -P1 INSERT ✕ 유니크 위반
```

주문 상태 변경(UPDATE, `@Version` 검사)은 커밋 직전에야 DB로 가므로, 두 요청 모두 같은 결제 키를 만들어 INSERT까지 갑니다. **결제 키 유니크 제약이 두 번째를 막아서 PG는 한 번만 불렸습니다(돈은 안전).** 하지만 그 위반이 `DataIntegrityViolationException`이라 처리되지 않은 예외로 분류돼 500이 됐습니다.

### 해결

주문 상태 변경을 결제 기록보다 **먼저** DB에 보냅니다(`orderRepository.saveAndFlush(order)`). 늦은 요청은 결제 기록을 만들기 전에 주문의 `@Version` 충돌로 끝나고, 이 충돌은 이미 409("다른 요청에 의해 상태가 변경되었습니다")로 처리하고 있었습니다. 주문 상태 변경은 낙관적 락이라는 설계 규칙 그대로이고, 비관적 락을 새로 넣지 않았습니다. 결제 키 유니크 제약은 마지막 안전장치로 남깁니다.

### 결과

같은 주문, 서로 다른 키, 결제 10건 동시 (`OrderApiTest.payDoubleClickWithDifferentKeys`):

| DB | Before | After |
|---|---|---|
| H2 | 승인 1건, 나머지 **9건 모두 500** (3회 동일) | 승인 1건, 나머지 9건 모두 **409** (5회 연속) |
| MySQL 8.0 (Testcontainers) | 승인 1건, 409 7건, **500 2건** | 승인 1건, 나머지 9건 모두 **409** (3회 연속) |

After에서는 결제 기록 1건, 승인 1건, 주문 PAID, 재고 1개 차감까지 함께 확인합니다. 전체 테스트는 H2와 MySQL 8.0 모두 통과했습니다(MySQL 165개).

**H2와 MySQL의 Before가 다른 이유**: MySQL은 두 번째 INSERT가 첫 트랜잭션의 잠금을 기다리는 동안 일부 요청이 이미 바뀐 주문(PAYING)을 읽어 상태 전이 오류(409)로 끝났습니다. H2만 봤다면 "MySQL에서는 덜 터진다"를, MySQL만 봤다면 "운이 나쁘면 모두 터진다"를 놓쳤을 것입니다. 같은 테스트를 두 DB에서 돌리는 이유입니다.

### 화면: 응답이 끊긴 뒤 다시 누르면 같은 키 (2026-11-26)

**문제**: 주문(`ProductDetailPage`)과 결제(`MyActivityPage`)가 클릭마다 `crypto.randomUUID()`로 새 키를 만들었습니다. 서버는 주문을 만들었는데 응답이 유실되면, 사용자가 다시 누를 때 새 키가 나가 서버는 다른 요청으로 보고 **주문을 한 번 더** 만들었습니다.

**해결**: 키를 "클릭 하나"가 아니라 "요청 하나"에 묶었습니다(`frontend/src/idempotency.ts`의 `IdempotencyKeys`). 결과를 모르는 실패(네트워크 오류, 5xx, 409 I003, 본문 읽기 실패)면 키를 남겨 같은 요청 재시도에 쓰고, 성공하거나 서버가 명확히 거절(그 밖의 4xx)하면 버립니다. 키 범위에는 요청 내용(주문은 상품·수량, 결제는 주문 id)을 넣어 같은 키로 다른 본문을 보내는 422를 피합니다. I003이 두 번 이어지면 아래 "남은 한계"의 IN_PROGRESS 잔존으로 보고 키를 버립니다(계속 막히지 않게).

| e2e (`page.route`로 첫 응답을 끊고 다시 누름) | Before | After |
|---|---|---|
| 주문: 재시도 요청의 키 | 다름 | **같음** |
| 주문: 생긴 주문 | **2건** (12, 13) | **1건** (재시도 응답 `Idempotent-Replayed: true`, 같은 주문 id) |
| 결제: 재시도 요청의 키 (첫 요청은 서버 전에 끊음) | 다름 | **같음**, 200 |

화면 결제하기 버튼의 같은 틱 두 번 클릭은 `useRef` 가드로 막았습니다(결제 요청 2회 → 1회, [PR #23](https://github.com/parkhh16/needs-shop/pull/23)).

**화면 쪽 한계**: 키는 화면 메모리에만 있어 페이지를 떠났다 오면 새 키가 됩니다. 수량을 바꿔 다시 누르는 것도 다른 요청이므로 새 키입니다.

## 남은 한계

- `IN_PROGRESS` 상태로 서버가 죽으면 그 키는 계속 409(I003)를 받습니다. 일정 시간이 지난 `IN_PROGRESS` 레코드를 정리하는 작업이 필요합니다. 화면은 I003이 두 번 이어지면 키를 버려 막히지 않게 했습니다.
- 주문 처리는 커밋됐는데 바로 뒤 완료 기록(`complete`)이 실패하면 레코드가 지워져, 같은 키로 재시도해도 주문이 하나 더 생길 수 있습니다(DB 장애 순간의 좁은 창).

## 관련 코드

- [`IdempotencyService`](../../backend/src/main/java/com/needsshop/idempotency/IdempotencyService.java)
- [`IdempotencyRecord`](../../backend/src/main/java/com/needsshop/idempotency/IdempotencyRecord.java): `UNIQUE(user_id, idempotency_key)`
- [`PaymentTxService.prepare`](../../backend/src/main/java/com/needsshop/payment/PaymentTxService.java): 주문 상태 변경을 결제 기록보다 먼저 flush
- [`idempotency.ts`](../../frontend/src/idempotency.ts): 화면의 키 보관(결과를 모르는 실패면 재사용)
- 테스트: `OrderApiTest` (`doubleClick`, `replay`, `keyReusedWithDifferentBody`, `payDoubleClickWithDifferentKeys`), `frontend/e2e/e2e.mjs` 2-b(응답 유실 후 재시도)
