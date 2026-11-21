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
