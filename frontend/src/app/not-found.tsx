import type { Metadata } from "next";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export const metadata: Metadata = { title: "페이지를 찾을 수 없어요 — Re:Ad" };

/** 없는 주소로 들어왔을 때 (Next.js 기본 영어 화면 대신) */
export default function NotFound() {
  return (
    <div className="mx-auto w-full max-w-[632px] px-4 py-24">
      <p className="text-sm leading-5 font-bold text-faint">404</p>
      <h1 className="mt-2 text-[28px] leading-[38px] font-bold tracking-[-0.035em] text-ink">
        페이지를 찾을 수 없어요
      </h1>
      <p className="mt-3 text-base leading-6 text-sub">주소가 바뀌었거나 없는 페이지예요.</p>
      <div className="mt-9 flex gap-2.5">
        <Link href="/" className={cn(buttonVariants({ size: "cta", variant: "secondary" }))}>
          처음으로
        </Link>
        <Link
          href="/checks/new"
          className={cn(
            buttonVariants({ size: "cta" }),
            "flex-1 bg-signal-green-strong hover:bg-signal-green-strong/90",
          )}
        >
          검수하러 가기
        </Link>
      </div>
    </div>
  );
}
