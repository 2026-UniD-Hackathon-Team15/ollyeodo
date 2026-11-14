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
| 프론트엔드 | Next.js 16 (App Router) · TypeScript · Tailwind CSS · shadcn/ui · TanStack Query |
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

> 백엔드 초기 세팅 이슈(#1)가 머지되면 백엔드 실행 방법을 채웁니다.

### 프론트엔드 (`frontend/`)

Node.js 20.9 이상이 필요합니다.

```bash
cd frontend
npm install
cp .env.example .env.local   # 처음 한 번
npm run dev                   # http://localhost:3000
```

- `NEXT_PUBLIC_USE_MOCK=true` 이면 백엔드 없이 목업 데이터(§9.1 응답 예시)로 화면이 동작합니다. 실제 API를 쓰려면 `false`로 바꾸고 `NEXT_PUBLIC_API_BASE_URL`을 백엔드 주소로 맞춥니다.
- 화면: `/menus/new` 메뉴 등록 · `/checks/new` 검수하기 · `/checks/{id}` 검수 결과 (목업: `/checks/12`)
- 검사: `npm run lint`, `npm run build`

### 환경변수

`.env.example`을 복사해 `.env`를 만들고 실제 값을 채웁니다. **`.env`는 커밋하지 않습니다.**

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
