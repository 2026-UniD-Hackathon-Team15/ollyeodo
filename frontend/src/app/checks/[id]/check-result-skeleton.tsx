import { Skeleton } from "@/components/ui/skeleton";

export function CheckResultSkeleton() {
  return (
    <div className="space-y-6" aria-busy="true" aria-label="검수 결과 불러오는 중">
      <Skeleton className="h-28 w-full rounded-xl" />
      <div className="grid gap-4 sm:grid-cols-2">
        <Skeleton className="h-36 rounded-xl" />
        <Skeleton className="h-36 rounded-xl" />
      </div>
      <Skeleton className="h-40 w-full rounded-xl" />
    </div>
  );
}
