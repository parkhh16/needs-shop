# 디자인 토큰 (1라운드)

대상 파일: `frontend/src/index.css`의 `:root`와 `@media (prefers-color-scheme: dark)` 블록. 구조는 그대로 두고 값과 이름만 바꾼다.
다크 모드는 `prefers-color-scheme`만 따른다. 수동 토글은 이번 범위 밖이다(다음 라운드).
새 라이브러리, 외부 웹폰트는 쓰지 않는다.

## 0. 원칙

1. 기존 변수 이름(`--bg`, `--surface`, `--surface-2`, `--border`, `--text`, `--muted`, `--accent`, `--accent-weak`, `--good*`, `--warn*`, `--bad*`, `--info*`)은 **유지**한다. `admin.css`와 관리자/내 주문 화면이 이 이름을 쓰므로, 이름을 바꾸면 다음 라운드 범위의 화면이 깨진다. 값만 바꾸고, 필요한 이름만 추가한다.
2. 색 대비는 아래 표의 계산값이 기준이다. 이 문서는 큰 글자 예외를 쓰지 않고 모든 글자를 4.5:1 이상으로 맞춘다. 아이콘, 입력 테두리, 포커스 링 같은 비텍스트는 3:1 이상.
3. 색만으로 상태를 구분하지 않는다. 상태는 항상 글자(예: "결제 확인 중")와 함께 쓰고, 확정되지 않은 상태는 점선 테두리로 확정 상태와 모양도 다르게 한다.
4. 빨강(`--bad`)은 "에러/실패"와 "파괴적 동작"에만 쓴다. 할인율은 빨강이 아니라 `--accent-text`로 쓴다.

## 1. 지금 무엇이 문제였나 (계산값)

측정은 axe 베이스라인(155개 중 33개 실패, 거의 전부 `color-contrast`)에 맞춰 `index.css`와 `admin.css`의 색 조합을 직접 계산했다. 아래 "원인"은 코드와 계산으로 추정한 것이다. 건수는 구현 후 axe를 다시 돌려 확인해야 한다.

| 요소 | 현재 조합 | 대비비 | 판정 |
|---|---|---|---|
| 라이트: `button` 기본(구매하기, 검색, 가입하기, 헤더 회원가입 `.header-cta`) | `#fff` / `#e8590c` | 3.58 | 실패 (4.5 필요) |
| 라이트: `.auth-alt a`, `.corrected a`, `.chip.static`, `.role-admin` (accent 글자) | `#e8590c` / 흰색 3.58, `--bg` 3.34, `--accent-weak` 3.24 | 3.24~3.58 | 실패 |
| 라이트: `.spec-table th` (상품 정보 표 머리) | `--muted #6b7280` / `--surface-2 #f0f2f5` | 4.31 | 실패 (4행이라 4건) |
| 라이트: `.demo-box .muted` (로그인 데모 안내) | `#6b7280` / `--surface-2` | 4.31 | 실패 |
| 라이트: `.badge.good`, `.s-paid` 등 good 글자 | `#2b8a3e` / `#ebfbee` 4.07, `--bg` 4.07 | 4.07 | 실패 |
| 라이트: `.s-paying`, `.badge.warn` warn 글자 | `#b35c00` / `#fff4e0` | 4.33 | 실패 |
| 다크: `button` 기본, `.header-cta`, `.buy`, `.auth-submit` | `#fff` / `#ff8a3d` | **2.35** | 실패 (가장 심각) |
| `.side-item.empty` (0개 소분류) | `opacity: .5` 로 글자가 흐려짐 | 약 1.97 | 실패 위험 (소분류를 펼쳐야 보여서 베이스라인엔 안 잡혔을 수 있음) |
| 라이트: `.muted` on `--bg` | `#6b7280` / `#f6f7f9` | 4.51 | 겨우 통과(여유 없음) |

axe 건수와 맞춰 보면, product(라이트) 6건 = 헤더 회원가입 1 + 구매하기 1 + `.spec-table th` 4, login(라이트) 3건 = 헤더 회원가입 + `.auth-alt a` + 데모 안내, signup(라이트) 3건 = 헤더 회원가입 + `.auth-alt a` + 가입하기, shop 2건 = 헤더 회원가입 + 검색 버튼, 다크의 1~2건은 흰 글자/주황 버튼이다. 대부분 **주황 버튼 흰 글자 하나(3.58)** 가 여러 화면에서 반복된 것이다.

핵심 결정: 라이트의 주황을 어둡게(`#c2410c`, 흰 글자 5.18), 다크의 주황 버튼에는 흰 글자 대신 어두운 글자(`#111317`, 7.93)를 쓴다.

## 2. 색 (라이트 / 다크 쌍)

`--accent`는 "면(버튼 배경, 포커스 링)"에, `--accent-text`는 "글자/링크"에 쓴다. 버튼 위 글자는 `--on-accent`.

| 변수 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `--bg` | `#f6f7f9` | `#111317` | 페이지 바탕 (변경 없음) |
| `--surface` | `#ffffff` | `#1a1d23` | 카드, 헤더, 입력칸 (변경 없음) |
| `--surface-2` | `#f0f2f5` | `#242932` | 보조 면, 이미지 자리, 표 머리 (다크만 `#22262e`→`#242932`) |
| `--border` | `#e2e5ea` | `#2e333d` | 장식용 구분선 (변경 없음) |
| `--border-strong` (신규) | `#838b99` | `#6b7280` | 입력칸/체크박스 테두리 (비텍스트 3:1) |
| `--text` | `#1a1d23` | `#e8eaed` | 본문 (변경 없음) |
| `--muted` | `#596273` | `#a0a8b5` | 보조 글자 (라이트 `#6b7280`→`#596273`, 다크 `#9aa1ad`→`#a0a8b5`) |
| `--accent` | `#c2410c` | `#ff8a3d` | 버튼 배경, 포커스 링, 진행 표시 (라이트만 변경) |
| `--accent-hover` (신규) | `#9a3412` | `#ffa05c` | 버튼 hover |
| `--on-accent` (신규) | `#ffffff` | `#111317` | accent 배경 위 글자 |
| `--accent-text` (신규) | `#9a3412` | `#ffa05c` | accent 계열 글자/링크, 할인율 |
| `--accent-weak` | `#fff1e8` | `#3a2414` | 칩/배지 배경 (변경 없음) |
| `--focus` (신규) | `var(--accent)` | `var(--accent)` | 포커스 링 |
| `--disabled-bg` (신규) | `#e4e7ec` | `#2a2f39` | 비활성 버튼 배경 |
| `--disabled-fg` (신규) | `#5f6878` | `#98a1af` | 비활성 버튼 글자 |
| `--on-solid` (신규) | `#ffffff` | `#ffffff` | 어두운 단색 칩(품절) 위 글자 |

### 상태 색 (글자 / 배경 / 테두리)

색 하나가 한 가지 의미만 갖도록 한다. 같은 호박색을 "재고 적음"과 "결제 확인 중"이 같이 쓰던 문제를 없앤다(결제 확인 중은 보라 계열로 분리).

| 의미 | 변수 | 라이트 글자 / 배경 | 다크 글자 / 배경 | 어디에 |
|---|---|---|---|---|
| 성공 | `--good`, `--good-weak` | `#1b6e31` / `#e6f6ea` | `#69db7c` / `#16301c` | 결제 완료 `PAID`, 아이디 사용 가능 |
| 주의 (재고 적음) | `--warn`, `--warn-weak` | `#8a4b00` / `#fff1d6` | `#ffc078` / `#3a2a12` | "N개 남음" |
| 에러 | `--bad`, `--bad-weak` | `#b42318` / `#fdecea` | `#ff8787` / `#3b1a1a` | 폼 에러, 실패, 파괴적 동작 |
| 정보 (대기) | `--info`, `--info-weak` | `#15578f` / `#e3f1fb` | `#74c0fc` / `#13283b` | 결제 대기 `PENDING_PAYMENT` |
| 확정 전 (신규) | `--unc`, `--unc-weak` | `#5b3fb5` / `#efeafc` | `#c4b5fd` / `#2a2250` | **결제 확인 중 `PAYING`, 결제 `UNKNOWN`** |
| 중립 (신규) | `--neutral`, `--neutral-weak` | `#4b5563` / `#e9ecf0` | `#a0a8b5` / `#2a2f39` | 만료, 취소 |
| 품절 (신규) | `--soldout-bg` + `--on-solid` | `#3f4650` / `#ffffff` | `#4a525c` / `#ffffff` | 품절 배지 (기존 하드코딩 `#495057` 대체) |

테두리: 별도 변수는 만들지 않고 상태 글자 변수를 그대로 쓴다(`border: 1px dashed var(--unc)`).

**확정 전 상태(PAYING, UNKNOWN) 표시 규칙**
- 글자: PAYING은 "결제 확인 중", 결제 UNKNOWN은 "확인 중"(현재 `AdminOrdersPage` 문구 유지).
- 모양: `border: 1px dashed var(--unc)` + 배경 `--unc-weak` + 글자 `--unc`. 실선 배지(완료/실패)와 모양이 달라야 한다.
- 점 하나(8px 원, 속이 빈 링)를 글자 앞에 둔다. 애니메이션은 `prefers-reduced-motion: no-preference`일 때만 부드러운 깜빡임, 아니면 정적.
- 이 상태의 주문 카드에는 취소 버튼, 재결제 버튼을 두지 않는다(현재 코드대로 안내 문구만).
- `.s-paying`은 `--unc` 쪽으로 바꾼다. `.s-unknown`(결제 상태용)도 같은 규칙. 다음 라운드(내 주문/관리자)에서 적용하지만 클래스는 이번에 정의해 둬도 된다.

## 3. 대비 계산표

`python`으로 WCAG 상대 휘도 공식을 직접 계산한 값이다(가장 낮은 값 기준).

### 라이트

| 글자/요소 | 배경 | 대비 | 기준 |
|---|---|---|---|
| `--text #1a1d23` | `--bg` | 15.75 | 4.5 |
| `--muted #596273` | `--bg` / `--surface` / `--surface-2` | 5.73 / 6.14 / 5.38 | 4.5 |
| `--on-accent #fff` | `--accent #c2410c` | 5.18 | 4.5 |
| `--on-accent #fff` | `--accent-hover #9a3412` | 7.31 | 4.5 |
| `--accent-text #9a3412` | `--bg` / `--surface` / `--accent-weak` | 6.82 / 7.31 / 6.61 | 4.5 |
| `--accent #c2410c` (포커스 링) | `--bg` / `--surface` | 4.83 / 5.18 | 3 |
| `--good #1b6e31` | `--good-weak` / `--surface` | 5.64 / 6.32 | 4.5 |
| `--warn #8a4b00` | `--warn-weak` / `--surface` | 6.09 / 6.80 | 4.5 |
| `--bad #b42318` | `--bad-weak` / `--surface` / `--bg` | 5.75 / 6.57 / 6.13 | 4.5 |
| `--info #15578f` | `--info-weak` / `--surface` | 6.53 / 7.52 | 4.5 |
| `--unc #5b3fb5` | `--unc-weak` / `--surface` | 6.32 / 7.44 | 4.5 |
| `--neutral #4b5563` | `--neutral-weak` / `--surface` | 6.38 / 7.56 | 4.5 |
| `--on-solid #fff` | `--soldout-bg #3f4650` | 9.53 | 4.5 |
| `--disabled-fg #5f6878` | `--disabled-bg #e4e7ec` | 4.53 | 4.5 (비활성은 법적 면제지만 읽히게) |
| `--border-strong #838b99` | `--bg` / `--surface` / `--surface-2` | 3.20 / 3.43 / 3.06 | 3 |

### 다크

| 글자/요소 | 배경 | 대비 | 기준 |
|---|---|---|---|
| `--text #e8eaed` | `--bg` / `--surface` | 15.43 / 14.01 | 4.5 |
| `--muted #a0a8b5` | `--bg` / `--surface` / `--surface-2` | 7.76 / 7.04 / 6.09 | 4.5 |
| `--on-accent #111317` | `--accent #ff8a3d` | 7.93 | 4.5 |
| `--on-accent #111317` | `--accent-hover #ffa05c` | 9.24 | 4.5 |
| `--accent-text #ffa05c` | `--bg` / `--accent-weak` | 9.24 / 7.23 | 4.5 |
| `--accent #ff8a3d` (포커스 링) | `--surface` | 7.20 | 3 |
| `--good #69db7c` | `--good-weak` | 8.16 | 4.5 |
| `--warn #ffc078` | `--warn-weak` | 8.60 | 4.5 |
| `--bad #ff8787` | `--bad-weak` | 6.72 | 4.5 |
| `--info #74c0fc` | `--info-weak` | 7.67 | 4.5 |
| `--on-solid #fff` | `--soldout-bg #4a525c` | 7.92 | 4.5 |
| `--border-strong #6b7280` | `--surface` / `--bg` / `--surface-2` | 3.49 / 3.85 / 3.02 | 3 |
| `--unc #c4b5fd` | `--unc-weak` / `--surface` | 7.88 / 9.14 | 4.5 |
| `--neutral #a0a8b5` | `--neutral-weak` | 5.60 | 4.5 |
| `--disabled-fg #98a1af` | `--disabled-bg` | 5.15 | 4.5 |

## 4. 간격 (4px 기준)

| 변수 | 값 | 용도 |
|---|---|---|
| `--space-1` | 4px | 아이콘과 글자 사이 |
| `--space-2` | 8px | 칩 사이, 카드 내부 줄 간격 |
| `--space-3` | 12px | 폼 요소 안쪽 여백, 그리드 가로 간격(모바일) |
| `--space-4` | 16px | 기본 여백, 그리드 가로 간격(PC), 모바일 좌우 여백 |
| `--space-5` | 24px | 섹션 안 여백, 그리드 세로 간격 |
| `--space-6` | 32px | 섹션 사이 |
| `--space-7` | 48px | 페이지 아래 여백, 상세 섹션 사이 |

하드코딩된 `6px`, `10px`, `14px`, `20px` 같은 값은 가까운 토큰으로 맞춘다(새 값을 만들지 않는다).

## 5. 글자

폰트는 현재 스택 그대로(`'Pretendard', -apple-system, BlinkMacSystemFont, 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif`). 웹폰트를 새로 불러오지 않는다. Pretendard는 설치돼 있을 때만 적용된다.

| 변수 | 크기 | 용도 |
|---|---|---|
| `--fs-xs` | 12px | 카테고리 줄, 원가, 재고 안내, 카운트 (이보다 작게 쓰지 않는다. 현재 `.badge` 11px, `.role` 11px, `.status` 12px 중 11px은 12px로 올린다) |
| `--fs-sm` | 13px | 칩, 폼 라벨, 안내 문구 |
| `--fs-md` | 14px | 본문 보조, 표, 사이드바, 헤더 링크 |
| `--fs-base` | 16px | 본문, 가격, 버튼 |
| `--fs-lg` | 18px | 상세 섹션 제목, 합계 |
| `--fs-xl` | 22px | 페이지 제목(`h2`, 상품명 `h1`) |
| `--fs-2xl` | 26px | 상세 가격 |

굵기: `--fw-regular: 400`, `--fw-medium: 500`, `--fw-semibold: 600`, `--fw-bold: 700`, `--fw-black: 800`(가격, 브랜드만).
줄 높이: 본문 `1.5`(현재), 제목 `1.35`, 상세 설명 `1.7`. 가격, 수량, 개수는 `font-variant-numeric: tabular-nums`.

## 6. 모서리 / 그림자

| 변수 | 값 | 용도 |
|---|---|---|
| `--radius-sm` | 6px | 배지, 메뉴 항목 |
| `--radius` | 10px | 카드, 이미지, 입력칸(현재 8px→10px), 버튼 (변경 없음 10px) |
| `--radius-lg` | 14px | 로그인/가입 카드 |
| `--radius-pill` | 99px | 칩, 상태 배지 |

| 변수 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `--shadow-1` | `0 1px 2px rgb(0 0 0 / .08)` | `0 1px 2px rgb(0 0 0 / .5)` | 이미지 위 배지 |
| `--shadow-2` | `0 8px 24px rgb(0 0 0 / .12)` | `0 8px 24px rgb(0 0 0 / .55)` | 계정 메뉴, 토스트, 모바일 드로어 |

카드와 입력칸에는 그림자를 쓰지 않고 `1px solid var(--border)`만 쓴다.

## 7. 상호작용 토큰

- 포커스 링: 모든 `a`, `button`, `input`, `select`, `summary`에 `:focus-visible { outline: 2px solid var(--focus); outline-offset: 2px; }`. 현재는 `input:focus`/`select:focus`에만 있어서 버튼과 링크에는 키보드 포커스가 보이지 않는다.
- 입력칸: `border: 1px solid var(--border-strong)`. `aria-invalid="true"`면 `border-color: var(--bad)`.
- 비활성: `opacity: .45` 대신 `button:disabled:not([aria-busy="true"]) { background: var(--disabled-bg); color: var(--disabled-fg); border-color: var(--disabled-bg); cursor: not-allowed; }`. 투명도로 흐리게 하면 대비가 깨지고 다크에서 탁한 갈색이 된다(login 다크 스크린샷의 "로그인" 버튼).
- **처리 중(중복 클릭 방지) 버튼**: 제출/주문 중에는 `disabled`를 유지하되 `aria-busy="true"`를 같이 달고, 비활성 색이 아니라 `--accent-hover` 배경 + `--on-accent` 글자 + 앞에 16px 회전 링(CSS `border` 스피너)을 보여준다. 글자는 "주문 중…", "로그인 중…", "가입 중…"(현재 문구 유지). 사용자가 "눌렸고 진행 중"임을 알 수 있어야 한다. `cursor: progress`. 회전은 `prefers-reduced-motion`이면 멈춘다.
- 터치 목표: 모바일에서 버튼, 링크 메뉴, 아이콘 버튼은 최소 40x40px(주요 버튼 `.buy`, `.auth-submit`은 높이 48px).
- 모션: 기존 `.2s`, 스켈레톤 shimmer 유지. `prefers-reduced-motion`이면 전부 정지.

## 8. 실제 값의 위치

1라운드 때 이 자리에 `index.css` 상단에 붙여 넣을 CSS 전체와 구현 체크리스트가 있었다. 모두 반영됐으므로 지웠다(원문: `git show 34421f2:docs/design/tokens.md`). **지금 값의 기준은 `frontend/src/index.css`**이고, 이 문서는 이름 규칙, 쓰임새, 대비 근거를 남긴다. 2라운드에서 바뀐 값은 11장에 있다.

## 10. 다음 라운드로 넘기는 영향 (이번에 화면을 고치지 않지만 알아둘 것)

- 관리자 화면(`admin.css`): `--accent`가 글자색으로 쓰이는 곳(`.link`, `.admin-tabs a.active` 테두리, `.s-demand-new`, `.bar-fill`)이 많다. 라이트에서 `--accent`가 `#c2410c`가 되어 대비는 모두 올라가지만(`.s-demand-new`는 `#c2410c` / `#fff1e8` = 4.69), 여유가 적으므로 글자에는 `--accent-text`로 바꾼다.
- 내 주문(`/me`)의 `.status`: `.s-paying`이 보라 계열로 바뀐다. 결제 확인 중 주문이 호박색에서 보라색으로 바뀌는 것이므로, 안내 문구("결제 결과를 확인하고 있어요")와 같이 확인한다.
- `.u-active`, `.u-locked`, `.c2/.c4/.c5`처럼 good/warn/bad를 쓰는 배지는 값만 바뀌며 대비는 모두 올라간다(good 4.07→5.64, warn 4.33→6.09).
- 다크 모드 수동 토글, 토스트 토큰화, 관리자 표의 모바일 레이아웃은 다음 라운드. → [backlog.md](backlog.md)

## 11. 2라운드에서 바뀐 것 (방향: "수량이 정해진 물건을 고르는 갤러리")

1라운드 토큰 이름은 모두 유지하고 값만 바꾸거나 새 이름을 더했다. 상태 색(`--good*`, `--warn*`, `--bad*`, `--info*`, `--unc*`)은 라운드 사이에 바꾸지 않는다. 값은 `index.css`에 있고, 여기에는 규칙과 근거만 둔다.

- **바탕**: 차가운 회색 → 따뜻한 종이(라이트 `--bg #faf8f5`) / 먹색(다크 `--bg #0f0e0c`). 다크 `--on-accent`는 `#0f0e0c`.
- **새 이름**: 히어로(`--hero-bg`, `--hero-fg`, `--hero-muted`, `--hero-accent`, `--hero-chip`), 품절 오버레이(`--scrim`, `--on-scrim`), 모양(`--radius-xl` 20px), 글자(`--fs-3xl` 32px, `--fs-hero` `clamp(28px, 5.2vw, 44px)`, `--ls-tight`, `--ls-hero`), 그림자(`--shadow-lift`, `--shadow-bar`), 모션(`--ease`, `--dur-1` .15s, `--dur-2` .25s), 모바일 높이(`--header-row` 56px, `--search-row` 52px, `--bar-h` 72px).
- **모서리**: `--radius-sm` 8px, `--radius` 12px, `--radius-lg` 16px(카드 이미지), `--radius-xl` 20px(구매 패널, 로그인 카드, 히어로).
- **주황(`--accent`)은 두 가지에만**: 행동(구매, 검색)과 한정·할인 신호(할인율, 남은 수량 칩).
- **그림자**: 평소에는 없고 hover(포인터 기기만), 모바일 하단 구매 바, 메뉴에서만.
- **아이콘**: 인라인 SVG, 24x24, `stroke="currentColor"`, 선 1.75, `aria-hidden="true"`. 이모지와 아이콘 라이브러리를 쓰지 않는다. 외부 폰트·이미지·CDN 없음.
- **모션**: 카드 hover 이미지 1.04배(`@media (hover: hover)`), 누르면 0.98배, 스켈레톤은 실제 카드와 같은 치수. 점멸, 펄스, 카운트다운 애니메이션, 반복 흔들림 금지. `prefers-reduced-motion`이면 전부 정지.
- **히어로 글로우**: `radial-gradient`의 accent 불투명도는 최대 30%. 넘기면 아래 표의 최악 점이 깨진다.

### 2라운드 대비 계산 (python, WCAG 상대 휘도, 가장 낮은 쌍 기준)

| 글자 / 요소 | 배경 | 라이트 | 다크 | 기준 |
|---|---|---|---|---|
| `--text` | `--bg` | 17.47 | 16.41 | 4.5 |
| `--text` | `--surface-2` | 16.02 | 13.95 | 4.5 |
| `--muted` | `--bg` | 6.94 | 7.58 | 4.5 |
| `--muted` | `--surface` | 7.35 | 7.09 | 4.5 |
| `--muted` | `--surface-2` | 6.36 | 6.44 | 4.5 |
| `--accent-text` | `--bg` | 6.89 | 9.59 | 4.5 |
| `--accent-text` | `--accent-weak` | 6.55 | 7.23 | 4.5 |
| `--on-accent` | `--accent` | 5.18 | 8.23 | 4.5 |
| `--hero-fg` | `--hero-bg` | 16.85 | 15.58 | 4.5 |
| `--hero-muted` | `--hero-bg` | 10.64 | 10.22 | 4.5 |
| `--hero-accent` | `--hero-bg` | 10.19 | 9.43 | 4.5 |
| `--hero-fg` | 히어로 글로우 최밝은 점(accent 30% 혼합: `#5f391f` / `#6a3c1e`) | 9.41 | 8.64 | 4.5 |
| `--hero-muted` | 같은 글로우 점 | 5.94 | 5.67 | 4.5 |
| `--hero-accent` | 같은 글로우 점 | 5.69 | 5.22 | 4.5 |
| `--hero-fg` | `--hero-chip` 위(`#35322e`) | 11.95 | - | 4.5 |
| `--on-scrim` | `--scrim`을 흰 사진 위에 올린 최악의 경우(`#575552`) | 7.43 | 7.43 | 4.5 |
| `--warn` | `--warn-weak` (재고 칩) | 6.09 | 8.60 | 4.5 |
| `--border-strong` | `--bg` / `--surface` | 3.58 / 3.79 | 4.24 / 3.97 | 3 (비텍스트) |

히어로 글로우는 `radial-gradient`의 accent 불투명도를 **최대 30%**로 제한한다. 이를 넘기면 위 표의 최악 점이 깨진다.
