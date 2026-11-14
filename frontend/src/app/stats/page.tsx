import type { Metadata } from "next";
import { Suspense } from "react";
import { RequireAuth } from "@/components/require-auth";
import { StatsView } from "./stats-view";

export const metadata: Metadata = { title: "검수 기록 — Re:Ad" };

export default function StatsPage() {
  return (
    <Suspense>
      <RequireAuth>
        <StatsView />
      </RequireAuth>
    </Suspense>
  );
}
