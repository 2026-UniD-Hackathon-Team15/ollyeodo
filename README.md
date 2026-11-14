# 올려도 될까

> AI로 만든 메뉴 홍보물을 **우리 가게 실물과 대조해** 과장과 오류를 찾아주는 검수 서비스
> Uni-DTHON 2026 · Team 15

사장님이 AI로 만든 메뉴 이미지와 인스타 문구를 실제 메뉴 정보·실물 사진과 비교해 항목별 🟢🟡🔴 신호와 **근거 법령 조항**을 보여주고, 고쳐서 다시 만들 수 있는 재생성 프롬프트와 캡션 수정안을 함께 돌려줍니다.

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
| 프론트엔드 | 미정 |
| 배포 | Docker → Render 또는 AWS EC2 |

## 레포 구조 (예정)

```text
.
├─ ollyeodo-backend/   Spring Boot API (com.team.ollyeodo)
├─ frontend/           화면 3개 (메뉴 등록 · 검수 요청 · 검수 결과)
├─ docs/               팀 규칙 등 보조 문서
├─ OLLYEODO_MVP.md
└─ .env.example
```

## 로컬 실행

Java 21을 설치한 뒤 저장소 루트에서 실행합니다. 로컬 DB는 H2이며,
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

Spring AI 1.1.8의 ChatClient 공통 의존성을 포함합니다.
AI 제공자는 MVP §16에서 미정이므로 제공자 starter와 실제 호출 연결은 #7에서 추가합니다.

### 환경변수

Spring Boot는 `.env`를 자동으로 읽지 않습니다. 필요한 값은 IDE 실행 설정이나
프로세스 환경변수로 전달하며, 비밀값을 코드나 설정 파일에 직접 적지 않습니다.

Supabase·AI 연동 시 `.env.example`을 복사해 `.env`를 만들고 실제 값을 채웁니다. **`.env`는 커밋하지 않습니다.**

```bash
cp .env.example .env
```

- `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_DB_PASSWORD`, AI API 키는 **서버 전용**입니다. 프론트 코드와 Git에 절대 넣지 않습니다.

## 협업 규칙 요약

- 브랜치 흐름: `feat/#이슈번호-설명` → **`develop`** (PR) → **`main`** (통합 확인 후 PR)
- 이슈 하나 = 브랜치 하나 = PR 하나. 작업 브랜치는 `develop`에서 따고 `develop`으로 PR합니다.
- 이슈 본문의 **선행 이슈**가 머지된 뒤 시작합니다.
- **작은 단위로 자주 커밋. force push·이력 삭제 금지** (대회 코드 검증 요건)
- 11/15 10:30 코드 프리즈 이후에는 버그 수정만 합니다.
