# Re:Ad — 프론트엔드

Next.js 16 (App Router) · TypeScript · Tailwind CSS v4 · shadcn/ui · TanStack Query

## 실행

```bash
npm install
cp .env.example .env.local   # 처음 한 번
npm run dev                   # http://localhost:3000
```

| 환경 변수 | 설명 |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | 백엔드 주소 (기본 `http://localhost:8080`) |
| `NEXT_PUBLIC_USE_MOCK` | `true`면 백엔드 없이 목업 데이터로 동작 (배포할 때는 `false`) |

`NEXT_PUBLIC_` 값은 브라우저에 노출됩니다. 비밀 키는 절대 넣지 않습니다.

## 구조

```
src/
├─ app/
│  ├─ page.tsx          홈
│  ├─ checks/new/       검수하기 — 메뉴 이름 · 실제 메뉴 사진 · AI 메뉴 사진 · 메뉴 설명 + 분석 중 로딩 (#15)
│  ├─ checks/[id]/      검수 결과 🔴 고쳐 주세요 / 🟢 올려도 됩니다 + 고친 문구 · 다시 만들 프롬프트 (#16)
│  ├─ not-found.tsx     없는 주소 화면
│  └─ error.tsx         오류 화면
├─ components/
│  ├─ home/             홈 화면
│  ├─ traffic-light.tsx 신호등 공통 컴포넌트
│  └─ ui/               shadcn/ui 컴포넌트 (button · skeleton · sonner)
├─ lib/
│  ├─ types.ts          API 타입 (OLLYEODO_MVP.md §8·§9)
│  ├─ severity.ts       신호 라벨·규칙 이름·안내 문구
│  └─ api/              API 클라이언트(client.ts) · React Query 훅(queries.ts)
└─ mocks/               목업 데이터와 목업 API
```

## API

`docs/check-api.md` 기준. 프론트가 부르는 API는 3개입니다 (메뉴 등록 API는 없습니다).

| 메서드 | 경로 | 쓰는 곳 |
|---|---|---|
| POST | `/api/checks` | 검수하기 — multipart `realImage` · `aiImage` · `caption` · 선택 `asOf` |
| GET | `/api/checks/{id}` | 검수 결과 |
| GET | `/api/rules` | 결과 화면의 판정 기준표 · 법령 원문 링크 |

데모용으로 `/checks/new?asOf=2026-11-26` 처럼 주소에 기준일을 붙이면 그날 시행 규칙으로 판정합니다.

## 규칙

- 화면 문구에 "위법/불법/법 위반" 같은 단정 표현을 쓰지 않습니다. "이 조항에 걸릴 수 있는 차이"로 씁니다.
- 판정은 백엔드 코드가 합니다. 프론트는 받은 결과를 보여주기만 합니다.
