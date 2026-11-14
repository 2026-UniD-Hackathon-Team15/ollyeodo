import { cn } from "@/lib/utils";
import { severityMeta } from "@/lib/severity";
import type { Severity } from "@/lib/types";

const tone: Record<Severity, string> = {
  GREEN: "bg-signal-green-soft text-signal-green-ink",
  YELLOW: "bg-signal-yellow-soft text-signal-yellow-ink",
  RED: "bg-signal-red-soft text-signal-red-ink",
};

const lamp: Record<Severity, string> = {
  GREEN: "bg-signal-green",
  YELLOW: "bg-signal-yellow",
  RED: "bg-signal-red",
};

/** 작은 신호 배지 — 항목 카드 등에 사용 */
export function SeverityBadge({
  severity,
  label,
  className,
}: {
  severity: Severity;
  /** 기본 라벨 대신 쓸 문구 (예: "직접 확인") */
  label?: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-bold",
        tone[severity],
        className,
      )}
    >
      <span aria-hidden className={cn("size-1.5 rounded-full", lamp[severity])} />
      {label ?? severityMeta[severity].label}
    </span>
  );
}

/** 신호등에 켤 불 — 결과 신호(RED·GREEN)와 "비교하고 있어요" 화면의 노란불(COMPARING) */
export type SignalState = Severity | "COMPARING";

const order: Severity[] = ["RED", "YELLOW", "GREEN"];

/**
 * 가로 3구 신호등 — 피그마 Signal v2 (11:107 RED · 11:111 YELLOW · 11:115 GREEN), 74×28.
 *
 * 디자인 기준: 결과 화면은 빨강·초록만 켠다. 노란불은 분석 중("비교하고 있어요") 화면에서만
 * `state="COMPARING"`으로 켜고 깜빡인다. 판정 값(§6 severity)은 그대로 두고 보여 주는 방식만 정한다.
 */
export function TrafficLight({
  state,
  size = "md",
  className,
}: {
  state: SignalState;
  size?: "md" | "lg";
  className?: string;
}) {
  const on: Severity = state === "COMPARING" ? "YELLOW" : state;
  const label = state === "COMPARING" ? "비교하고 있어요" : severityMeta[state].label;
  return (
    <span
      role="img"
      aria-label={`신호: ${label}`}
      className={cn(
        "inline-flex shrink-0 items-center rounded-full bg-ink",
        size === "md" ? "h-7 w-[74px] justify-between px-[9px]" : "h-10 w-[106px] justify-between px-[13px]",
        className,
      )}
    >
      {order.map((s) => (
        <span
          key={s}
          className={cn(
            "rounded-full",
            size === "md" ? "size-3.5" : "size-5",
            s === on
              ? cn(lamp[s], state === "COMPARING" && "motion-safe:animate-signal-blink")
              : "bg-white/[0.13]",
          )}
        />
      ))}
    </span>
  );
}
