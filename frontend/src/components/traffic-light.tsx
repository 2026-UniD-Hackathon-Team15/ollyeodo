import { cn } from "@/lib/utils";
import { severityMeta } from "@/lib/severity";
import type { Severity } from "@/lib/types";

const tone: Record<Severity, string> = {
  GREEN: "bg-signal-green-soft text-signal-green border-signal-green/30",
  YELLOW: "bg-signal-yellow-soft text-signal-yellow border-signal-yellow/30",
  RED: "bg-signal-red-soft text-signal-red border-signal-red/30",
};

const lamp: Record<Severity, string> = {
  GREEN: "bg-signal-green",
  YELLOW: "bg-signal-yellow",
  RED: "bg-signal-red",
};

/** 작은 신호 배지 — 항목 카드 등에 사용 */
export function SeverityBadge({
  severity,
  className,
}: {
  severity: Severity;
  className?: string;
}) {
  const meta = severityMeta[severity];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold",
        tone[severity],
        className,
      )}
    >
      <span aria-hidden className={cn("size-2 rounded-full", lamp[severity])} />
      {meta.label}
    </span>
  );
}

/** 큰 3구 신호등 — 결과 화면 상단 종합 신호에 사용 */
export function TrafficLight({
  severity,
  className,
}: {
  severity: Severity;
  className?: string;
}) {
  const order: Severity[] = ["RED", "YELLOW", "GREEN"];
  return (
    <div
      role="img"
      aria-label={`종합 신호: ${severityMeta[severity].label}`}
      className={cn(
        "inline-flex flex-col gap-2 rounded-2xl bg-neutral-900 p-2.5 shadow-inner",
        className,
      )}
    >
      {order.map((s) => (
        <span
          key={s}
          className={cn(
            "size-8 rounded-full transition-opacity",
            lamp[s],
            s === severity ? "opacity-100 shadow-[0_0_16px] shadow-current" : "opacity-20",
          )}
        />
      ))}
    </div>
  );
}
