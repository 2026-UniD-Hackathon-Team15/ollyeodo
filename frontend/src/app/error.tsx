"use client";

import Link from "next/link";
import { useEffect } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** 화면을 그리다 오류가 났을 때 (Next.js 기본 영어 화면 대신). 헤더·푸터는 그대로 보인다 */
export default function Error({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="mx-auto w-full max-w-[632px] px-4 py-24">
      <h1 className="text-[28px] leading-[38px] font-bold tracking-[-0.035em] text-ink">
        화면을 불러오지 못했어요
      </h1>
      <p className="mt-3 text-base leading-6 text-sub">잠시 뒤 다시 시도해 주세요.</p>
      <div className="mt-9 flex gap-2.5">
        <Link href="/" className={cn(buttonVariants({ size: "cta", variant: "secondary" }))}>
          처음으로
        </Link>
        <Button
          size="cta"
          onClick={reset}
          className="flex-1 bg-signal-green-strong hover:bg-signal-green-strong/90"
        >
          다시 시도
        </Button>
      </div>
    </div>
  );
}
