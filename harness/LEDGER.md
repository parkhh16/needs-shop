# LEDGER
## 01 점검
- 스크린샷 캡처 가능, build/lint OK. e2e는 states 없이 216/4, `npm run states` 후 218/2(admin-activity 모바일 scrollable-region-focusable).
- README 공백: Backend/Frontend "선택 이유" 빈 칸, docs/images 없음, 이슈 번호 [#N]과 파일 01–14 불일치, 장바구니 화면 없음.
## 02 홈 레일
- 남은 수량 레일을 한 줄 가로 스크롤로 변경(PC 4+2 고아 줄 제거). 홈 전체 높이 4143→3764px. build/lint 24/e2e 218/2.
## 03 상품 상세
- 설명 끝 "(1인 N개)" 중복 표기 제거(화면에서만). build/lint 24/e2e 218/2.
- 스크린샷 전후 이미지는 컨테이너 소실로 없음(재생성 필요).
## N1 회수 반영
- claude/gallant-babbage-c2dyh7의 02·03 커밋과 harness 문서를 relay/needs-shop에 fast-forward로 반영.
- 확인: build 통과, lint 경고 24개. e2e는 이번 회차에 돌리지 않음(02/03 기록의 218/2를 따름).
- 전후 스크린샷(가로 800px 이하): docs/design/shots/r3-before, r3-after (shop PC 라이트·모바일 다크, product PC 라이트·모바일 다크). shop PC 라이트 전체 높이 2589→2352px(800px 폭으로 축소한 값).
- 남은 문제: 상품 상세는 높이가 같아 보여(PC 1969, 모바일 3755px) 중복 표기 제거 효과를 이미지로 판단하기 어려움. 육안 비교 필요.
## N2 (04·05)
- 04: 면접관 시점 리뷰, 지적 3건(중2/하1)을 BATON에 기록. 코드 수정 없음.
- 05: 후기 없음(.card-rating.none)을 regular 굵기로, PC 대표(2x2) 카드 이미지를 cover→contain(+패딩)으로 변경. 인기 순위 뱃지도 이제 보임.
- 확인: build 통과, lint 경고 24. e2e는 이번 회차에 돌리지 않음. 전후: docs/design/shots/relay-n2/{before,after} (shop.pc.light 대표 카드, detail.* 추천 영역, 가로 800px 이하).
- 남은 문제: 대표 카드 contain에서 흰 사진이 베이지 배경 위에 네모로 보임(배경색 정리 여지). 모바일 홈 간격은 미착수. detail 추천 영역의 전후 차이는 눈에 띄지 않음.
## N3 (06 주문·결제 화면)
- /me 주문 안내 문구만 수정: PAYING에 "취소·재결제 불가, 결과가 나오면 자동으로 바뀜" 명시(보라 점선 박스 유지, 버튼 없음), 결제 대기 기한을 줄 분리·굵게, 기한 만료에 재주문 안내 추가. 로직·api.ts·멱등 키 변경 없음.
- 확인: build 통과, lint 경고 24, e2e 통과 218 / 실패 2(admin-activity 모바일 라이트·다크 scrollable-region-focusable, 기준과 동일). '결제 버튼 더블클릭 결제 요청 1회' PASS(요청 1회), 모바일 구매 더블클릭 1회 PASS.
- 전후: docs/design/shots/relay-n3/{before,after}/me-orders.* (PC 라이트, 모바일 다크). 주문은 page.route로 5개 상태를 모의한 화면이다(서버에 실제 PAYING 주문을 만들지 않음).
- 미착수: 입력 검증 메시지(로그인·가입), 재고 부족 표시는 서버 메시지를 그대로 보여주는 구조라 프론트 문구 변경 없음. 필요하면 07 이후에 확인.
- 남은 문제: 04에서 지적한 결제 버튼 useRef 가드, 멱등 키 재사용은 이번 지시(로직 금지)에 따라 미수정.
## N4 (07 접근성)
- axe scrollable-region-focusable: 원인은 AdminActivityPage의 `.table-wrap`(가로 스크롤, 포커스 불가). tabIndex=0, role=region, aria-label 추가로 해결.
- 모바일 상세: 정사각 사진 때문에 390x844 첫 화면에서 수량 행이 y 813~863으로 고정 구매 바(상단 772) 뒤에 있었음. 모바일 갤러리를 4:3 + contain으로 낮춰 716~766으로 올라와 바 위에 보임. 스크롤 하단 여백(detail-page padding)은 원래 충분했음.
- 로그인 포커스링: 테두리 1px과 바깥 링 2px(offset 0)이 겹쳐 이중 테두리. outline-offset -1px + border-color 동일로 단일 링.
- 확인: build 통과, lint 경고 24, e2e 통과 220 / 실패 0(직전 218/2에서 개선). 전후: docs/design/shots/relay-n4/{before,after}(detail-qty, login-focus, 모바일 라이트·다크).
- 남은 문제: 높이 640 이하 화면에서는 첫 화면에 수량 행이 여전히 바 아래(스크롤하면 보임). 후기 섹션 접근성은 미확인. 모바일 상세 사진이 contain이라 사진 좌우에 베이지 여백이 생김.
## N5 (08+09 README)
- 코드 수정 없음. README.md만 변경. '주요 결과' 표 숫자는 그대로.
- 30초 요약(문제, 해결 핵심 3가지, 대표 수치)을 '프로젝트 목표' 위에 추가. 수치는 기존 표의 값(74개, TPS 24~32 → 110~133, 초과 판매 0건)만 사용.
- 선택 이유: Backend, Frontend 두 칸을 채움. 근거는 build.gradle·코드(`@Version`, `@Modifying`, `@DynamicUpdate`, 라우트, `tsc -b`, Playwright/axe)에 실제로 있는 것만.
- 이슈 번호 정리: README는 #1~#18, 파일은 01~14라 어긋났다(예: #13이 09-auth, #17이 13-scale-out). 번호를 파일 번호로 통일해 `[이슈 NN]`으로 바꾸고, 한 파일에 속한 하위 주제(01의 #1·#2, 02의 #3·#4, 04의 #6·#7, 06의 #9·#10)는 한 줄로 합쳐 14줄로 만듦. 본문 안 참조(기술 스택 표 #13→이슈 09, #17→이슈 13, 한계 절 #15→이슈 11, #1 문서→이슈 01 문서)도 같이 수정.
- Mermaid: 스케줄 주기(대사 10s, 만료 5s, 태그 1m, 활동 로그 300ms 배치, 등록 추천 하루 1번 cron)와 주문 상태 전이(OrderStatus.next)를 코드와 대조했고 일치해 수정하지 않음.
- 사실 정정: Flyway 'V1~V9'는 실제 V1~V11이라 수정. 'CI 테스트 135개'는 확인할 근거가 없어(`@Test` 어노테이션은 163개로 실행 건수와 다름) 숫자를 뺌.
- 링크: README의 상대 링크 전부 실제 파일 존재 확인(깨진 것 없음). 이미지 링크는 README에 없음(docs/images 없음).
- 남은 문제: ERD에 product_review, demand_report, shedlock 테이블이 없음(틀린 것은 아니라 두었음). README에 스크린샷 이미지가 없음. 실제 테스트 실행 건수는 이번에 측정하지 않음.
## N6 (10 최종 스크린샷)
- 코드 수정 없음. `npm run states` 후 `npm run shots`(56장)로 최종 상태를 찍고, 대표 4장(홈·상세 × PC 라이트·모바일 다크)만 가로 800px 이하로 줄여 docs/design/shots/final/에 저장. 네 장 모두 이미지가 실제로 로드된 것을 눈으로 확인.
- 기준선은 릴레이 시작 전 main(4d472ec)에서 찍은 r3-before를 사용. 높이(줄인 이미지 기준): 홈 PC 2589→2352, 상세 PC 1969→1682, 상세 모바일 3755→3280, 홈 모바일 5670→5670(변화 없음).
- docs/design/shots/BEFORE_AFTER.md에 기준선→최종 표, 회차별(02,03,05,06,07) 전/후 경로 표, 보이는 변화가 없는 회차(04, 08+09, axe)를 정리. 표의 모든 경로가 실제 파일을 가리키는 것을 확인.
- README '화면 구성' 위에 final 이미지 4장을 상대 경로로 삽입하고 BEFORE_AFTER.md 링크 추가.
- 남은 문제: 03(구매 제한 중복 제거)은 높이가 같아 이미지로 구분이 어렵다. 홈 모바일 다크는 높이 변화가 없다. 11 최종 리뷰, 12 블로그 글감이 남음.
## N7 (11 최종 점검) — 전체 요약
### 회차 표
| 회차 | 한 일 | 결과 |
|---|---|---|
| 01 | 점검 | build/lint OK, e2e 216/4 → states 후 218/2 |
| 02 | 홈 레일 한 줄 가로 스크롤 | 홈 PC 높이 4143→3764(원 측정), 이후 비교 2589→2352(줄인 이미지) |
| 03 | 상세 구매 제한 중복 표기 제거 | 화면에서만 제거 |
| N1 | 02·03 회수 반영 + 전후 스크린샷 | r3-before/after |
| N2 | 04 면접관 리뷰, 05 대표 카드·후기 없음 | 지적 3건(중2, 하1), 대표 카드 contain |
| N3 | 06 주문·결제 화면 문구 | PAYING 안내 명확화, 더블클릭 1회 PASS |
| N4 | 07 접근성·모바일 수량 행·포커스링 | e2e 218/2 → 220/0 |
| N5 | 08+09 README | 30초 요약, 선택 이유, 이슈 번호 통일 |
| N6 | 10 최종 스크린샷 | final/, BEFORE_AFTER.md |
| N7 | 11 최종 점검, 꼬리질문 | docs/interview-questions.md |
### 최종 수치 (이번 회차에 실제 실행)
- `npm run build` 통과, `npm run lint` 경고 24개(기준 24 이하), `npm run e2e`(states 후) 통과 220 / 실패 0(직전 기준 218/2보다 나아짐).
- 구매 버튼: 모바일 상세 DOM 1개 PASS, 후기 섹션에서도 하나 PASS. `inFlight` ref 가드(ProductDetailPage.tsx 20·70·71·85행) 유지, 소스의 `className="buy"` 2개는 로그인 여부에 따라 하나만 렌더되는 분기. PAYING: 보라 점선 박스, '결제 확인 중', 취소·재결제 버튼 없음(MyActivityPage.tsx).
- main 대비 변경: backend, api.ts, Flyway, deploy, .github 변경 없음. frontend는 index.css, admin.css, MyActivityPage.tsx, ProductDetailPage.tsx, AdminActivityPage.tsx 5개(20줄 추가, 13줄 삭제). frontend/docs/harness 밖에서는 README.md만 변경(문서).
### 못 한 것
- 12 블로그 글감 초안, 05b 모바일 홈 간격, 후기 섹션 접근성 확인.
- 04 지적 2건(결제 버튼 useRef 가드, 멱등 키 재사용)은 로직 변경 금지라 미수정.
- 높이 640 이하 모바일에서 첫 화면의 수량 행은 여전히 구매 바 아래.
- docs/guide.md의 'V1~V9', '전체 135개'가 틀린 채 남음. docs/issues/ 안에 옛 번호 참조 `[#13]`, `[#14]` 등이 남음(README는 파일 번호로 통일됨).
- 실제 테스트 실행 건수와 백엔드 테스트(`./gradlew test`)는 이번 릴레이에서 실행하지 않음.
### 사람이 할 일
- 브랜치 relay/needs-shop 검토 후 PR 생성(릴레이 중에는 만들지 않음).
- 면접 꼬리질문 문서(근거 없음으로 표시한 곳)를 직접 보강, 04 지적 2건의 수정 여부 결정.
- README의 이미지와 docs/guide.md, docs/issues의 옛 번호 정리.
### 교훈 후보 (LESSONS.md는 고치지 않음)
- 스크린샷은 800px 이하로 줄이고 대표 장만 보면 읽기 비용이 작다.
- 미커밋 변경 없이 회차마다 커밋·push하면 Stop 훅 재호출을 막는다.
- 기능 변경 없는 문구 수정도 e2e와 더블클릭 가드 확인을 같이 돌리면 회귀를 빨리 잡는다.
- 번호처럼 여러 문서가 서로 참조하는 값은 한 번에 정리하지 않으면 어긋남이 남는다(README만 고치고 docs/issues를 못 고침).

## 관리자 도움말(?) 정리 — 대시보드·주문 관리·상품 관리 (세 페이지 범위)
- 한 일: `HelpTip`("?" 호버·포커스·탭 도움말) 컴포넌트 추가, 관리자 소개 문단을 한 줄 요약+"?"로 분리, 브라우저 기본 `title` 호버 설명을 `HelpTip`으로 교체, 메뉴·표 글자 크기 조정.
- 확인: build 통과, lint 경고 24, e2e 통과 220 / 실패 0(axe 포함). 전후: docs/design/shots/relay-admin-help/{before,after}.
- 회귀 1건 발견·수정: 표 글자를 키워 상품 재고 입력칸의 "-출고" 안내문이 잘림 → 입력칸 폭 110→128px.
- 남은 것: 활동 로그·회원·등록 추천 페이지의 안쪽 설명(소개 문단은 공통 레이아웃이라 이미 적용). 모바일 상품 표 오른쪽이 잘리는 기존 문제(이번 변경과 무관). 전후 대시보드 수치는 데이터가 달라서 비교 대상이 아님.

## 관리자 도움말(?) 정리 — 활동 로그·회원 관리·등록 추천 (나머지 세 페이지)
- 활동 로그: 결과·traceId 열 머리글에 "?" 추가.
- 회원 관리: 잠김 배지의 브라우저 title 제거 → "?" 도움말, 관리 열 머리글에 정지·강제 로그아웃 설명 "?".
- 등록 추천: 시연용 데이터 안내, "지금 다시 정리" 대기 시간 안내(title 제거), 정리 시점, 묶이지 않은 검색어, 집계 방식을 한 줄 + "?"로 축소.
- 검증: build 통과, lint 경고 24, e2e 218/2 (실패 2건은 변경 전에도 동일: /products/2 "N개 남음", /products/4 품절 버튼 — 이 작업과 무관, 별도 확인 필요).
- 전후 이미지: docs/design/shots/relay-admin-help2/{before,after}
