import type { Metadata } from "next";
import { PageHeader } from "@/components/page-header";
import { Card, CardContent } from "@/components/ui/card";

export const metadata: Metadata = { title: "메뉴 등록 — 올려도 될까" };

// 화면 1 (OLLYEODO_MVP.md §11) — 폼 구현은 이슈 #14
export default function NewMenuPage() {
  return (
    <>
      <PageHeader
        title="메뉴 등록"
        description="실제 메뉴 정보와 실물 사진을 등록하면, AI 홍보물을 이 기준과 비교해요."
      />
      <Card>
        <CardContent className="py-10 text-center text-sm text-muted-foreground">
          메뉴명 · 재료와 개수 · 층 수 · 원산지 · 실물 사진 입력 폼이 들어갈 자리
        </CardContent>
      </Card>
    </>
  );
}
