# 팀 규칙

## 대회 요건 (어기면 감점·수상 제외)

- **커밋 이력 유지 필수**: force push, rebase로 이력 지우기, 레포 재생성 금지
- 연습용 test 레포 코드를 복사하지 않는다 (기존 코드와 80% 이상 유사 시 부정행위 검토)
- 작은 단위로 자주 커밋한다
- 11/15(일) **10:30 코드 프리즈** → 이후 버그 수정만

## 브랜치

```text
feat/#8-comparison-service ─┐
feat/#14-menu-form ─────────┼─► develop ──(통합 확인 후)──► main
fix/#9-asof-timezone ───────┘
```

- `main`: 최종 제출·배포용. **`develop`에서 통합 확인이 끝난 것만** PR로 합친다. 직접 push 금지
- `develop`: 통합 브랜치. 모든 작업 브랜치는 **`develop`에서 따고 `develop`으로 PR**한다. 직접 push 금지
- 작업 브랜치: `feat/#이슈번호-설명`, `fix/#이슈번호-설명`
  - 예: `feat/#8-comparison-service`
  - 시작: `git switch develop && git pull && git switch -c feat/#8-comparison-service`
- `develop → main` 머지: 체크포인트(`OLLYEODO_MVP.md` §12.1)마다 PM이 올린다. 11/15 10:30 코드 프리즈 직전에 마지막으로 올린다
- 제출용 zip은 `main`의 최종 커밋으로 만든다
- 브랜치 이름은 **영어**로 짓는다 (한글은 Windows 터미널 인코딩·배포 도구에서 깨질 수 있음). 한국어는 커밋 메시지·PR·이슈에 쓴다

### 종류

| 접두어 | 용도 |
|---|---|
| `feat/` | 기능 구현 |
| `chore/` | 세팅·배포 등 기능이 아닌 작업 |
| `test/` | 점검·테스트 |
| `fix/` | 버그 수정 (코드 프리즈 이후에는 이것만) |
| `docs/` | 문서 |

### 이슈별 추천 브랜치 이름

| # | 이슈 | 역할 | 선행 이슈 | 브랜치 |
|---:|---|---|---|---|
| 1 | Spring Boot 프로젝트 초기 세팅 | BE | — | `chore/#1-backend-setup` |
| 2 | 프론트엔드 프로젝트 초기 세팅 + 목업 API | FE | — | `chore/#2-frontend-setup` |
| 3 | Supabase 대회용 프로젝트 연결 + Storage | BE | #1 | `chore/#3-supabase-storage` |
| 4 | JPA 엔티티 4개 + jsonb 매핑 + RLS | BE | #3 | `feat/#4-jpa-entities` |
| 5 | ⚖️ 규칙 데이터 13개 적재 (근거 조항·시행일) | BE | #4 | `feat/#5-rule-seed-data` |
| ~~6~~ | ~~메뉴 API~~ — ❌ 닫음 (2026-11-14 스코프 변경) | — | — | — |
| 7 | AI 속성·캡션 주장 추출 (ExtractionService) | BE | #1 | `feat/#7-ai-extraction` |
| 8 | ⚖️ 이미지 비교 판정 ComparisonService | BE | #1 | `feat/#8-comparison-service` |
| 9 | ⚖️ 캡션 검수 + RuleEngine (asOf 판정) | BE | #5, #8 | `feat/#9-caption-rule-engine` |
| 10 | 재생성 프롬프트 RegenPromptBuilder | BE | #9 | `feat/#10-regen-prompt` |
| 11 | 검수 API `POST/GET /api/checks` | BE | #7, #9, #10 | `feat/#11-check-api` |
| 12 | ⚖️ 규칙 API `GET /api/rules` | BE | #5 | `feat/#12-rule-api` |
| 13 | 백엔드 Docker + 배포 | BE | #11 | `chore/#13-backend-deploy` |
| ~~14~~ | ~~메뉴 등록 화면~~ — ❌ 닫음 (2026-11-14 스코프 변경) | — | — | — |
| 15 | 검수 요청 화면 + 로딩 | FE | #2 | `feat/#15-check-request-page` |
| 16 | ⚖️ 검수 결과 + 재생성 프롬프트 화면 | FE | #2 | `feat/#16-check-result-page` |
| 17 | 실 API 연동 | FE | #11, #12, #15, #16 | `feat/#17-api-integration` |
| 18 | 프론트엔드 배포 | FE | #13, #17 | `chore/#18-frontend-deploy` |
| 19 | ⚖️ 데모 시나리오 E2E 점검 | BE+FE | #13, #18 | `test/#19-demo-e2e` |
| 20 | (권장) 검수 기록 통계 API | BE | #11 | `feat/#20-check-stats-api` |
| 21 | (권장) 검수 기록 통계 화면 | FE | #2, #20 | `feat/#21-check-stats-page` |

## 커밋 메시지

```text
feat: 이미지 개수 비교 판정 추가 (#8)
fix: asOf 기본값을 Asia/Seoul로 수정 (#9)
docs: README 실행 방법 추가
test: ComparisonService 경계값 테스트 (#8)
```

## PR

- **base 브랜치는 `develop`** (`main`으로 PR 금지. `develop → main` 통합 PR만 예외)
- 본문에 `Closes #이슈번호`
- ⚖️ `legal` 라벨 이슈는 **판정 기준표(OLLYEODO_MVP.md §6)와 일치하는지** 리뷰어가 확인한 뒤 머지
- 리뷰어 1명 승인 후 머지 (Squash 금지 — 커밋 이력 유지를 위해 Merge commit 사용)

## 라벨

| 라벨 | 의미 |
|---|---|
| `backend` / `frontend` | 담당 영역 |
| `⚖️ legal` | 법령 근거 판정 규칙을 다루는 이슈. 임계값·근거 조항 임의 변경 금지 |
| `P0 필수` / `P1 권장` | 코드 프리즈 전 필수 / 시간 남으면 |
| `setup` | 프로젝트·인프라 세팅 |
| `blocked` | 선행 이슈 미완료. **선행 이슈가 모두 닫히면 자동으로 떨어진다** (`.github/workflows/blocked-label.yml`) |
