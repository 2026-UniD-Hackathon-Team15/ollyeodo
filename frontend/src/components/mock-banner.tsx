import { env } from "@/lib/env";

/** 목업 모드일 때만 보이는 띠 — 데모·개발 중 혼동 방지 (피그마에는 없는 개발용 표시) */
export function MockBanner() {
  if (!env.useMock) return null;
  return (
    <div className="bg-ink py-1 text-center text-xs text-white/70">
      목업 모드 — 백엔드 없이 예시 데이터로 동작 중 (NEXT_PUBLIC_USE_MOCK=true)
    </div>
  );
}
