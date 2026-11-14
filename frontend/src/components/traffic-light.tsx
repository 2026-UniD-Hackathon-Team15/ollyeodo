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

/** 신호등에 켤 불 — 결과 신호(RED·GREEN)와 "비교하고 있어요"(COMPARING: 빨강·초록 번갈아 깜빡임) */
export type SignalState = Severity | "COMPARING";

/**
 * 가로 2구 신호등 (빨강·초록) — 결과가 🔴·🟢 두 가지뿐이라 노란불을 뺐다 (§6, #33).
 * 로고의 「:」(위 빨강·아래 민트)와 같은 두 불이다.
 *
 * - RED / GREEN: 그 불만 켠다 (결과 화면·홈 예시)
 * - COMPARING: 분석 중 — 빨강·초록이 번갈아 깜빡인다
 * 판정 값(§6 severity)은 그대로 두고 보여 주는 방식만 정한다. YELLOW 가 와도 노란불은 켜지 않는다.
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
  const comparing = state === "COMPARING";
  const label = comparing ? "비교하고 있어요" : severityMeta[state].label;
  const lamps = [
    { color: lamp.RED, lit: comparing || state === "RED", blink: "motion-safe:animate-signal-stop" },
    { color: lamp.GREEN, lit: comparing || state === "GREEN", blink: "motion-safe:animate-signal-go" },
  ];
  return (
    <span
      role="img"
      aria-label={`신호: ${label}`}
      className={cn(
        "inline-flex shrink-0 items-center rounded-full bg-ink",
        size === "md" ? "h-7 gap-1.5 px-[9px]" : "h-10 gap-2 px-[13px]",
        className,
      )}
    >
      {lamps.map((l) => (
        <span
          key={l.color}
          className={cn(
            "rounded-full",
            size === "md" ? "size-3.5" : "size-5",
            l.lit ? cn(l.color, comparing && l.blink) : "bg-white/[0.13]",
          )}
        />
      ))}
    </span>
  );
}
