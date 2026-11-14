import type { Metadata } from "next";
import { Suspense } from "react";
import { RequireAuth } from "@/components/require-auth";
import { CheckResultView } from "./check-result-view";
import { CheckResultSkeleton } from "./check-result-skeleton";

export const metadata: Metadata = { title: "검수 결과 — Re:Ad" };

// 화면 3 (OLLYEODO_MVP.md §11) — 데모 주인공 화면. 섹션마다 회색 띠가 화면 끝까지 닿아서 PageContainer 를 쓰지 않는다.
export default function CheckResultPage() {
  return (
    <RequireAuth>
      <Suspense fallback={<CheckResultSkeleton />}>
        <CheckResultView />
      </Suspense>
    </RequireAuth>
  );
}
