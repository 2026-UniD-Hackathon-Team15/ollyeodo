import type { Metadata } from "next";
import { NewCheckForm } from "./new-check-form";

export const metadata: Metadata = { title: "검수하기 — Re:Ad" };

// 화면 2 (OLLYEODO_MVP.md §11) 검수 요청 + 분석 중 로딩
export default function NewCheckPage() {
  return <NewCheckForm />;
}
