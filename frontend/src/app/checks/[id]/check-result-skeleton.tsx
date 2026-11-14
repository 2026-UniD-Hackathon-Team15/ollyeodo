import { Skeleton } from "@/components/ui/skeleton";

/** 결과를 불러오는 동안 — 결과 화면(피그마 13:169)과 같은 자리에 회색 상자 */
export function CheckResultSkeleton() {
  return (
    <div
      className="mx-auto w-full max-w-[632px] space-y-4 px-4 pt-12 pb-24"
      aria-busy="true"
      aria-label="검수 결과 불러오는 중"
    >
      <Skeleton className="h-7 w-[74px] rounded-full" />
      <Skeleton className="h-[92px] w-3/4 rounded-xl" />
      <Skeleton className="h-6 w-2/3 rounded-lg" />
      <div className="grid grid-cols-2 gap-2.5 pt-3">
        <Skeleton className="aspect-[295/221] rounded-2xl" />
        <Skeleton className="aspect-[295/221] rounded-2xl" />
      </div>
    </div>
  );
}
