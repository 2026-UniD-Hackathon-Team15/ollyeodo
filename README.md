# 올려도 될까

> AI로 만든 메뉴 홍보물을 **우리 가게 실물과 대조해** 과장과 오류를 찾아주는 검수 서비스
> Uni-DTHON 2026 · Team 15

사장님이 AI로 만든 메뉴 이미지와 홍보 문구를 **실제 음식 사진**과 비교해 🟢 올려도 됨 · 🔴 고쳐 주세요 결과와 🔴 항목별 **사유·근거 법령 조항**을 보여주고, 고쳐서 다시 만들 수 있는 개선 프롬프트와 문구 수정안을 함께 돌려줍니다.

> [!IMPORTANT]
> **⚖️ 판정 기준은 법령에 근거합니다.** 모든 규칙에는 근거 조항과 시행일이 붙고, 판정은 AI가 아니라 코드(RuleEngine)가 합니다.
> 우리는 위법 여부를 판단하지 않고, "이 조항에 걸릴 수 있는 차이"를 참고 신호로 알려줍니다.
> 판정 규칙·임계값·근거 조항은 [`OLLYEODO_MVP.md`](./OLLYEODO_MVP.md) §6 판정 기준표가 유일한 기준입니다.

## 문서

| 문서 | 내용 |
|---|---|
| [`OLLYEODO_MVP.md`](./OLLYEODO_MVP.md) | **MVP 단일 기준 문서** — 범위, 근거 법령, 판정 기준표, API, DB, 화면, 일정, 이슈 연결 |
| [`docs/team-rules.md`](./docs/team-rules.md) | 브랜치·커밋·PR 규칙 (대회 요건 포함) |

## 기술 스택

| 영역 | 선택 |
|---|---|
| 백엔드 | Java 21, Spring Boot 3.x, Spring Data JPA, Spring AI |
| DB·스토리지 | Supabase (PostgreSQL, Storage) — Seoul 리전 |
| AI | 멀티모달 비전 모델 (미정: OpenAI / Gemini / Claude) |
| 프론트엔드 | Next.js 16 (App Router) · TypeScript · Tailwind CSS · shadcn/ui · TanStack Query |
| 배포 | Docker → Render 또는 AWS EC2 |

## 레포 구조 (예정)

```text
.
├─ ollyeodo-backend/   Spring Boot API (com.team.ollyeodo)
├─ frontend/           홈 + 화면 2개 (검수 요청 · 검수 결과)
├─ docs/               팀 규칙 등 보조 문서
├─ OLLYEODO_MVP.md
└─ .env.example
```

## 로컬 실행

### 백엔드 (`ollyeodo-backend/`)

Java 21이 필요합니다. 로컬 DB는 H2이며,
현재 초기 설정과 헬스 API 실행에는 외부 DB나 AI API 키가 필요하지 않습니다.

```bash
cd ollyeodo-backend
./gradlew bootRun
```

다른 터미널에서 확인합니다. 기본 포트는 8080입니다.

```bash
curl -i http://localhost:8080/api/health
```

정상 응답: HTTP 200, `{"status":"UP"}`. 서버 종료는 `Ctrl+C`입니다.

```bash
# ollyeodo-backend/에서 테스트 및 전체 빌드
./gradlew test
./gradlew build
```

CORS는 기본적으로 `http://localhost:3000`, `http://localhost:5173`을 허용합니다.
다른 프론트 주소는 `APP_CORS_ALLOWED_ORIGINS` 환경변수에 쉼표로 구분해 설정합니다.
이미지 병렬 추출에는 `extractionExecutor` 빈을 `CompletableFuture.supplyAsync`에 전달합니다.

Spring AI 1.1.8 OpenAI starter를 사용합니다. 저장소 루트 `.env`의
`AI_API_KEY`, `AI_MODEL`은 Spring Config Import로 로드됩니다.
모델 기본값은 `gpt-5.6-luna`이며 temperature는 기본 미설정으로 요청에서 생략합니다.
temperature를 지원하는 모델로 변경하면 `AI_TEMPERATURE`(예: `0`) 또는
`spring.ai.openai.chat.options.temperature`를 설정해 선택적으로 적용할 수 있습니다.
Spring AI 자체의 기본값 0.7도 미설정 시 전송하지 않도록 처리합니다.
실행 위치가 루트 또는 `ollyeodo-backend/`인 경우 모두 `.env`를 찾습니다.
키를 코드·명령줄 인자·Git에 넣지 않습니다.

실제 API 테스트는 `@Tag("live")`이며 일반 `test`/`build`에서 제외합니다.
`ollyeodo-backend/src/test/resources/demo/`의 `strawberry-cake.png` 또는
`strawberry-cake.jpg`를 읽습니다. PNG를 우선 사용하며 둘 다 없으면 skip합니다.
이미지를 준비한 뒤 다음 명령을 실행하면 실제 API 호출 비용이 발생합니다.

```bash
cd ollyeodo-backend
./gradlew liveExtractionTest
```

같은 사진으로 4회 병렬 추출하고 캡션도 1회 추출합니다.
응답 시간은 로그에, 추출값과 일치 여부는 `build/reports/ai-extraction-demo.json`에 저장됩니다.
`APP_EXTRACTION_TIMEOUT`으로 HTTP·서비스 제한시간(기본 60s)을 조정할 수 있습니다.
키 없이 서버를 실행할 때는 `SPRING_AI_MODEL_CHAT=none`으로 AI 구성을 비활성화합니다.

## 협업 규칙 요약

- 브랜치 흐름: `feat/#이슈번호-설명` → **`develop`** (PR) → **`main`** (통합 확인 후 PR)
- 이슈 하나 = 브랜치 하나 = PR 하나. 작업 브랜치는 `develop`에서 따고 `develop`으로 PR합니다.
- 이슈 본문의 **선행 이슈**가 머지된 뒤 시작합니다.
- **작은 단위로 자주 커밋. force push·이력 삭제 금지** (대회 코드 검증 요건)
- 11/15 10:30 코드 프리즈 이후에는 버그 수정만 합니다.

### 추출 자기일관성 비교

추출 순서는 **실물 먼저(A안, MVP §7.1)** 입니다. `extractImages(ai, real)`는
1) 실물 사진을 2회 병렬로 자유 추출하고, 2) 두 결과의 재료명 합집합으로 목록을 만든 뒤,
3) 그 목록을 넘겨 AI 이미지를 2회 병렬 추출합니다. 두 사진이 같은 재료명을 쓰게 해서
`IMG_EXTRA_ITEM` "없는 재료" 오판을 막습니다. 실물에서 재료가 하나도 나오지 않으면
`ExtractionException`("실물 사진을 다시 찍어 주세요")을 던지고 AI 이미지는 호출하지 않습니다.
메뉴 설명(캡션)은 `extractCaption`으로 따로 추출합니다.

위치 설명·접시·장식 설명은 일치 여부에 사용하지 않습니다.
`agreement.items`는 재료별 두 개수와 일치 여부를 담습니다. null·누락·중복 응답은 해당 재료만
확인 불가로 처리합니다. 추가 재료 판정은 `ComparisonService`가 실물·AI 추출값을 비교해 수행합니다.

`plate`, `garnish`는 원본의 참고값으로 유지합니다. `layers`, `people`, `visible_text`는 계속 비교합니다.
MVP §6의 1.2/1.5배는 AI·실물 간 판정 임계값이며 추출 오차 허용값이 아닙니다.
추출의 `fill_ratio` 허용 오차 상수 `ImageExtractionResult.FILL_RATIO_TOLERANCE`는 미설정입니다.
동일 수치는 MATCH, 서로 다른 수치는 TOLERANCE_UNSET, 확인 불가 값은 UNKNOWN으로 반환합니다.
허용 오차가 결정된 경우 절대 차이가 허용값 이하인지 비교하도록 구현했습니다.
제안값은 0.05(5%p)이나 승인 전까지 적용하지 않습니다. §6 판정 임계값과 심각도는 변경하지 않습니다.

live 테스트는 동일 사진을 실물·AI 양쪽으로 넣어 A안 순서(실물 2회 → AI 2회)로 추출합니다.
