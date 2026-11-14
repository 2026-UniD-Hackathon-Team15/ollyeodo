import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/page-header";
import { buttonVariants } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { env } from "@/lib/env";

export const metadata: Metadata = { title: "검수하기 — 올려도 될까" };

// 화면 2 (OLLYEODO_MVP.md §11) — 폼·로딩 구현은 이슈 #15
export default function NewCheckPage() {
  return (
    <>
      <PageHeader
        title="검수하기"
        description="AI로 만든 메뉴 사진과 올릴 문구를 넣으면, 등록한 메뉴와 비교해 드려요."
      />
      <Card>
        <CardContent className="flex flex-col items-center gap-4 py-10 text-center text-sm text-muted-foreground">
          메뉴 선택 · AI 이미지 업로드 · 캡션 입력 · 기준 날짜 · 검수하기 버튼이 들어갈 자리
          {env.useMock && (
            <Link href="/checks/12" className={buttonVariants({ variant: "outline" })}>
              목업 결과 화면 보기
            </Link>
          )}
        </CardContent>
      </Card>
    </>
  );
}
