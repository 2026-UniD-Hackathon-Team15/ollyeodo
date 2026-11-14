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
| `NEXT_PUBLIC_USE_MOCK` | `true`면 백엔드 없이 목업 데이터로 동작 |

`NEXT_PUBLIC_` 값은 브라우저에 노출됩니다. 비밀 키는 절대 넣지 않습니다.

## 구조

```
src/
├─ app/
│  ├─ menus/new/        화면 1 메뉴 등록 (#14)
│  ├─ checks/new/       화면 2 검수하기 (#15)
│  └─ checks/[id]/      화면 3 검수 결과 + 재생성 프롬프트 (#16)
├─ components/
│  ├─ traffic-light.tsx 신호등·신호 배지 공통 컴포넌트
│  └─ ui/               shadcn/ui 컴포넌트
├─ lib/
│  ├─ types.ts          API 타입 (OLLYEODO_MVP.md §8·§9)
│  ├─ severity.ts       신호 라벨·규칙 이름·안내 문구
│  └─ api/              API 클라이언트(client.ts) · React Query 훅(queries.ts)
└─ mocks/               목업 데이터와 목업 API
```

## 규칙

- 화면 문구에 "위법/불법/법 위반" 같은 단정 표현을 쓰지 않습니다. "이 조항에 걸릴 수 있는 차이"로 씁니다.
- 판정은 백엔드 코드가 합니다. 프론트는 받은 결과를 보여주기만 합니다.
