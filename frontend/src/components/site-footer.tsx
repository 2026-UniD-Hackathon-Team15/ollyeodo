import { DISCLAIMER } from "@/lib/severity";
import { Logo } from "./logo";

/** 피그마 Footer v2 (11:100) */
export function SiteFooter() {
  return (
    <footer className="bg-surface-subtle">
      <div className="flex flex-col gap-4 px-4 pt-10 pb-12 sm:flex-row sm:items-end sm:justify-between sm:px-[50px]">
        <div className="flex flex-col gap-2.5">
          <Logo className="text-[17.6px]" />
          <p className="text-[13px] leading-[22px] text-faint">
            {DISCLAIMER} 판정 기준은 검수일 현행 법령이에요.
          </p>
        </div>
        <p className="text-[13px] leading-[22px] text-faint">Uni-DTHON 2026 · Team 15</p>
      </div>
    </footer>
  );
}
