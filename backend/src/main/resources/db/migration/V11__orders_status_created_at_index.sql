-- 인기 배지 집계(WHERE status = 'PAID' AND created_at >= ?)용. 쓰기 경로 인덱스가 하나 늘지만 상태·생성 시각 두 컬럼뿐이다
create index idx_orders_status_created_at on orders (status, created_at);
