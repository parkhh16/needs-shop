-- 상품 후기(별점). 지금은 시드로만 들어가는 샘플 데이터이고, 화면에는 읽기 전용으로 보여 준다.
-- H2(MySQL 모드)와 MySQL 8 양쪽에서 동작하도록 공통 문법만 사용한다.

create table product_review (
    id         bigint auto_increment primary key,
    product_id bigint       not null,
    rating     tinyint      not null,
    body       varchar(1000),                -- 별점만 남긴 후기는 null
    nickname   varchar(30)  not null,
    created_at datetime(6)  not null,
    constraint fk_product_review_product foreign key (product_id) references product (id),
    constraint ck_product_review_rating check (rating between 1 and 5)
);
-- 상품별 최신순 목록: WHERE product_id = ? ORDER BY created_at DESC 를 인덱스로 처리
create index idx_product_review_product_created on product_review (product_id, created_at);

-- 목록 카드의 평점을 후기 테이블 집계 없이(N+1 없이) 만들기 위한 비정규화 값. 평균 = rating_sum / review_count.
-- 후기가 시드로만 들어오므로 시드가 후기와 함께 채운다. (후기 작성 기능이 생기면 같은 트랜잭션에서 증감해야 한다)
alter table product add column review_count int not null default 0;
alter table product add column rating_sum int not null default 0;
