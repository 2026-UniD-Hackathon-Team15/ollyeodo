import type { Metadata } from "next";
import { Suspense } from "react";
import { CheckResultView } from "./check-result-view";
import { CheckResultSkeleton } from "./check-result-skeleton";

export const metadata: Metadata = { title: "검수 결과 — 올려도 될까" };

// 화면 3 (OLLYEODO_MVP.md §11) — 데모 주인공 화면, 디자인 완성은 이슈 #16
export default function CheckResultPage() {
  return (
    <Suspense fallback={<CheckResultSkeleton />}>
      <CheckResultView />
    </Suspense>
  );
}
