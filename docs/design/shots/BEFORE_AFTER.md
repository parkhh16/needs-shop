# 전/후 스크린샷 표

모든 이미지는 가로 800px 이하입니다(PC는 1280px 화면을 800px로 줄인 것). 높이 수치는 줄인 이미지 기준이며, 같은 도구(`npm run shots`)로 같은 계정·같은 데이터에서 찍었습니다. 기준선은 릴레이 시작 전 `main`(4d472ec)입니다.

## 릴레이 전체: 기준선 → 최종 (`final/`)

| 화면 | 전 (기준선) | 후 (최종) | 눈에 보이는 변화 |
|---|---|---|---|
| 홈 PC 라이트 | [r3-before/shop.pc.light.png](r3-before/shop.pc.light.png) (높이 2589) | [final/shop.pc.light.png](final/shop.pc.light.png) (높이 2352) | 남은 수량 레일이 한 줄 가로 스크롤로 바뀌어 4+2 고아 줄이 사라짐, 대표 카드 사진이 잘리지 않음 |
| 홈 모바일 다크 | [r3-before/shop.mobile.dark.png](r3-before/shop.mobile.dark.png) (높이 5670) | [final/shop.mobile.dark.png](final/shop.mobile.dark.png) (높이 5670) | 높이 변화 없음. 레일 카드 폭만 220→232px |
| 상세 PC 라이트 | [r3-before/product.pc.light.png](r3-before/product.pc.light.png) (높이 1969) | [final/product.pc.light.png](final/product.pc.light.png) (높이 1682) | 높이 1969→1682. 설명 끝 "(1인 N개)" 중복 표기가 사라짐(칩에만 표시) |
| 상세 모바일 다크 | [r3-before/product.mobile.dark.png](r3-before/product.mobile.dark.png) (높이 3755) | [final/product.mobile.dark.png](final/product.mobile.dark.png) (높이 3280) | 사진이 정사각→4:3으로 낮아져 수량 행이 구매 바 위로 올라옴 |

### 최종 화면 8장 (PC·모바일 × 라이트·다크)

README에는 맨 위만 잘라낸 `*.top.png`를 쓰고, 전체 화면은 `.top` 없는 파일입니다.

| 화면 | PC 라이트 | PC 다크 | 모바일 라이트 | 모바일 다크 |
|---|---|---|---|---|
| 홈 | [final/shop.pc.light.png](final/shop.pc.light.png) | [final/shop.pc.dark.png](final/shop.pc.dark.png) | [final/shop.mobile.light.png](final/shop.mobile.light.png) | [final/shop.mobile.dark.png](final/shop.mobile.dark.png) |
| 상품 상세 | [final/product.pc.light.png](final/product.pc.light.png) | [final/product.pc.dark.png](final/product.pc.dark.png) | [final/product.mobile.light.png](final/product.mobile.light.png) | [final/product.mobile.dark.png](final/product.mobile.dark.png) |

## 회차별

| 회차 | 한 일 | 전 | 후 | 변화 |
|---|---|---|---|---|
| 02 홈 레일 | 레일을 한 줄 가로 스크롤로 | [r3-before/shop.pc.light.png](r3-before/shop.pc.light.png) | [r3-after/shop.pc.light.png](r3-after/shop.pc.light.png) | PC 높이 2589→2352. 모바일은 높이 변화 없음 |
| 03 상세 | 구매 제한 중복 표기 제거 | [r3-before/product.pc.light.png](r3-before/product.pc.light.png) | [r3-after/product.pc.light.png](r3-after/product.pc.light.png) | 높이는 같음(1969). 설명 끝 문구만 달라짐, 이미지로는 구분이 어려움 |
| 05 대표 카드, 후기 없음 | 대표(2x2) 카드 사진 `cover`→`contain`, 후기 없음 굵기 | [relay-n2/before/shop.pc.light.png](relay-n2/before/shop.pc.light.png) | [relay-n2/after/shop.pc.light.png](relay-n2/after/shop.pc.light.png) | 사진 전체가 보이고 "인기" 뱃지가 보임 |
| 05 상세 추천 영역 | (같은 회차) | [relay-n2/before/detail.pc.light.png](relay-n2/before/detail.pc.light.png) | [relay-n2/after/detail.pc.light.png](relay-n2/after/detail.pc.light.png) | 변화 없음(눈에 띄는 차이 없음) |
| 06 내 활동 주문 | 주문 안내 문구, PAYING 안내 | [relay-n3/before/me-orders.pc.light.png](relay-n3/before/me-orders.pc.light.png) | [relay-n3/after/me-orders.pc.light.png](relay-n3/after/me-orders.pc.light.png) | PAYING에 "취소·재결제 불가" 안내, 기한 줄 분리(모바일 다크도 같은 폴더). 주문은 모의 응답으로 띄운 화면 |
| 07 모바일 상세 수량 행 | 사진 4:3 | [relay-n4/before/detail-qty.mobile.light.png](relay-n4/before/detail-qty.mobile.light.png) | [relay-n4/after/detail-qty.mobile.light.png](relay-n4/after/detail-qty.mobile.light.png) | 수량 행이 구매 바 뒤(y 813)에서 바 위(y 716)로 (다크도 같은 폴더) |
| 07 로그인 포커스링 | 이중 테두리 제거 | [relay-n4/before/login-focus.mobile.light.png](relay-n4/before/login-focus.mobile.light.png) | [relay-n4/after/login-focus.mobile.light.png](relay-n4/after/login-focus.mobile.light.png) | 이중 링이 단일 링으로 |

## 이전 라운드

- 1라운드: [r1-before](r1-before/) → [r1-after](r1-after/) (56장)
- 2라운드: [r2-after](r2-after/)

## 보이는 변화가 없는 회차

- 04 면접관 리뷰, 08+09 README: 화면 변경 없음(문서만).
- 07 axe `scrollable-region-focusable`: 화면 변화 없음(표 영역에 키보드 초점과 이름만 추가). e2e 통과 218/2 → 220/0.
