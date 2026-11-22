# BATON (다음 할 일)
- [x] 04 면접관 시점 리뷰 (지적 아래, 수정은 06 때 범위 판단)
- [x] 05 후기 없음 굵기, 대표(2x2) 카드 이미지 크롭 (모바일 홈 간격은 남음)
- [ ] 05b 모바일 홈 간격
- [x] 06 주문/결제 표시(PAYING 규칙 유지)
- [ ] 07 접근성(scrollable-region-focusable 포함), 모바일 상세 수량 행이 구매 바에 가림, 로그인 입력 포커스링 이중 테두리, 후기 섹션 확인
- [ ] 09 README(선택 이유, 이미지, 이슈 번호 정리)
- [ ] 10 스크린샷 재생성(전후)
- [ ] 11 최종 리뷰
- [ ] 12 블로그 글감 초안

## 04 면접관 지적 [심각도/파일:줄/증상/제안]
- [중/pages/MyActivityPage.tsx:47-55/결제 버튼 가드가 state(paying)뿐이라 같은 틱의 따닥 클릭이 둘 다 통과할 수 있음/구매 버튼처럼 useRef 가드 추가(프론트만으로 가능)]
- [중/pages/MyActivityPage.tsx:50, pages/ProductDetailPage.tsx:75/멱등 키를 클릭마다 새로 만들어, 응답이 타임아웃으로 유실된 뒤 재시도하면 서버가 중복을 못 알아봄/실패 시 키를 보관해 재사용(주문은 서버 쪽 확인 필요, 범위 밖일 수 있음)]
- [하/ProductDetailPage.tsx:75/crypto.randomUUID는 보안 컨텍스트(HTTPS, localhost)에서만 존재/운영은 HTTPS라 문제 없음, 문서에 전제만 적기]
- [OK/MyActivityPage.tsx:113-118/PAYING은 '결제 확인 중' 안내만 있고 취소·재결제 버튼 없음. 확정처럼 보이지 않음]
- 범위 이탈: 4d472ec 이후 frontend 변경은 index.css, ProductDetailPage.tsx뿐. backend/api.ts/deploy 변경 없음.
- docs/interview.md는 저장소에 없음(꼬리질문 참고 불가). 필요하면 anthropic-skills:interviewer로 생성.
