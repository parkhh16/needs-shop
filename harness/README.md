# 에이전트 릴레이 (UI/UX·README 개선)
- BATON.md: 다음 할 일, LEDGER.md: 회차별 기록. 회차마다 최상단 1~2개만 처리.
- 로컬 확인: `cd backend && ./gradlew compileJava`(Maven 429면 20초 뒤 재시도), `./gradlew bootRun`(H2, 8080)
- 프론트: `cd frontend && npm ci && npm run build && npm run lint`, `npm run dev`(5173)
- 스크린샷: `npm run shots -- <dir>`(56장, 약 80초). e2e: `npm run states && npm run e2e`
