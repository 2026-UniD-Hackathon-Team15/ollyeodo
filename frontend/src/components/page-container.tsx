import { cn } from "@/lib/utils";

/** 홈을 뺀 화면의 가운데 단 — 피그마 화면들의 본문 폭 600 */
export function PageContainer({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("mx-auto w-full max-w-[632px] px-4 pt-16 pb-24", className)}>
      {children}
    </div>
  );
}
