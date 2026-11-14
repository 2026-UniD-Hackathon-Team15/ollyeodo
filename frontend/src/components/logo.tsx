import { cn } from "@/lib/utils";

/**
 * Re:Ad 로고 — 피그마 Logo/Re:Ad (11:84).
 * 가운데 ":"는 신호등이다. 위는 빨강(멈춤), 아래는 민트(올려도 됨).
 * 크기는 글자 크기(font-size)를 따라 커진다. 기본 22px.
 */
export function Logo({
  className,
  animated = false,
  display = false,
}: {
  className?: string;
  /** true 면 ":" 두 점이 번갈아 깜빡인다 (홈 첫 화면용) */
  animated?: boolean;
  /** 큰 로고(홈 첫 화면·마무리)는 피그마처럼 ":"를 글자 대비 조금 작게 그린다 */
  display?: boolean;
}) {
  return (
    <span
      role="img"
      aria-label="Re:Ad"
      className={cn(
        "inline-flex items-center font-logo text-[22px] leading-[1.1] font-extrabold tracking-[-0.03em] text-ink",
        className,
      )}
    >
      <span aria-hidden>Re</span>
      <svg
        aria-hidden
        viewBox="0 0 9 16"
        className={cn(
          "shrink-0",
          display ? "h-[0.6em] w-[0.35em]" : "h-[0.727em] w-[0.409em]",
          animated && "logo-colon-animated",
        )}
      >
        <circle className="logo-stop" cx="4.5" cy="5.5" r="2.5" fill="var(--color-signal-red)" />
        <circle className="logo-go" cx="4.5" cy="13.5" r="2.5" fill="var(--color-signal-green)" />
      </svg>
      <span aria-hidden>Ad</span>
    </span>
  );
}
