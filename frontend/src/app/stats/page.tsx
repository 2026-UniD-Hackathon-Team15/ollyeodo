import type { Metadata } from "next";
import { StatsView } from "./stats-view";

export const metadata: Metadata = { title: "검수 기록 — Re:Ad" };

export default function StatsPage() {
  return <StatsView />;
}
