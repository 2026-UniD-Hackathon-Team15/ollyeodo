import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/page-header";
import { buttonVariants } from "@/components/ui/button";
import { PageContainer } from "@/components/page-container";
import { Card, CardContent } from "@/components/ui/card";
import { env } from "@/lib/env";

export const metadata: Metadata = { title: "검수하기 — Re:Ad" };

// 화면 2 (OLLYEODO_MVP.md §11) — 폼·로딩 구현은 이슈 #15
export default function NewCheckPage() {
  return (
    <PageContainer>
      <PageHeader
        title="검수할 메뉴를 넣어 주세요"
        description="직접 찍은 실제 메뉴 사진과 AI 메뉴 사진·설명을 대조해 드려요."
      />
      <Card>
        <CardContent className="flex flex-col items-center gap-4 py-10 text-center text-sm text-muted-foreground">
          메뉴 이름 · 실제 메뉴 사진 · AI 메뉴 사진 · 메뉴 설명 · 검수하기 버튼이 들어갈 자리
          {env.useMock && (
            <Link href="/checks/12" className={buttonVariants({ variant: "outline" })}>
              목업 결과 화면 보기
            </Link>
          )}
        </CardContent>
      </Card>
    </PageContainer>
  );
}
