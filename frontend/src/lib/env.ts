/**
 * 프론트 환경 변수. NEXT_PUBLIC_ 값은 브라우저에 그대로 노출되므로
 * 비밀 키(Supabase service role, AI 키 등)는 절대 여기 두지 않는다.
 */
export const env = {
  apiBaseUrl: process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080",
  /** "true"면 백엔드 없이 목업 데이터로 동작 */
  useMock: process.env.NEXT_PUBLIC_USE_MOCK === "true",
} as const;
