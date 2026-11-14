import type { Metadata } from "next";
import { Suspense } from "react";
import { PageContainer } from "@/components/page-container";
import { CheckResultView } from "./check-result-view";
import { CheckResultSkeleton } from "./check-result-skeleton";

export const metadata: Metadata = { title: "검수 결과 — Re:Ad" };

// 화면 3 (OLLYEODO_MVP.md §11) — 데모 주인공 화면, 디자인 완성은 이슈 #16
export default function CheckResultPage() {
  return (
    <PageContainer>
      <Suspense fallback={<CheckResultSkeleton />}>
        <CheckResultView />
      </Suspense>
    </PageContainer>
  );
}
