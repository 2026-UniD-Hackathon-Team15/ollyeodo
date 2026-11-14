"use client";

import { ChevronRight } from "lucide-react";
import Link from "next/link";
import { TrafficLight } from "@/components/traffic-light";
import { useCheckStats } from "@/lib/api/queries";
import { cn } from "@/lib/utils";
import type { RecentCheck, RuleCode, Severity } from "@/lib/types";

/** 피그마 「04 검수 기록」 항목 이름 (판정 기준표 이름과 다르게 짧게 줄인 표기) */
const topRuleTitle: Partial<Record<RuleCode, string>> = {
  IMG_COUNT: "재료 개수 차이",
  IMG_EXTRA_ITEM: "실물에 없는 재료",
  CAP_FACT: "문구 속 수치 불일치",
  CAP_MEDICINE: "의약품 오인 표현",
  CAP_SUPERLATIVE: "근거 없는 최상급",
};

const bowlIcon: Record<Severity, string> = {
  GREEN: "/stats/icon-bowl-green.svg",
  YELLOW: "/stats/icon-bowl-red.svg",
  RED: "/stats/icon-bowl-red.svg",
};

const thumbTone: Record<Severity, string> = {
  GREEN: "bg-signal-green-soft",
  YELLOW: "bg-signal-yellow-soft",
  RED: "bg-signal-red-soft",
};

const dateFormat = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
  timeZone: "Asia/Seoul",
});

export function StatsView() {
  const { data, isPending, isError } = useCheckStats();

  if (isPending) return <div className="min-h-[60vh]" />;
  if (isError) {
    return (
      <div className="mx-auto w-full max-w-[1040px] px-4 pt-14 text-[15px] text-sub sm:px-0">
        검수 기록을 불러오지 못했어요. 잠시 뒤 다시 시도해 주세요.
      </div>
    );
  }

  const maxCount = Math.max(...data.ruleCounts.map((r) => r.count), 1);
  const stats = [
    { label: "전체 검수", value: data.totalChecks, dot: null },
    { label: "올려도 됨", value: data.greenChecks, dot: "bg-signal-green" },
    { label: "고쳐야 함", value: data.redChecks, dot: "bg-signal-red" },
  ];

  return (
    <div className="mx-auto flex w-full max-w-[1040px] flex-col gap-10 px-4 pt-14 pb-24 sm:px-0">
      <header className="flex flex-col gap-2">
        <h1 className="text-[32px] leading-[44px] font-bold tracking-[-0.96px] text-ink">
          검수 기록
        </h1>
        <p className="text-[17px] leading-[27px] text-sub">
          지금까지 검수한 메뉴와 자주 걸리는 유형을 모아 봤어요.
        </p>
      </header>

      <section className="flex gap-4">
        {stats.map((s) => (
          <div
            key={s.label}
            className="flex min-w-0 flex-1 flex-col gap-1.5 rounded-[20px] bg-surface px-6 pt-6 pb-[22px]"
          >
            <div className="flex items-center gap-2">
              {s.dot && <span aria-hidden className={cn("size-2 rounded-full", s.dot)} />}
              <p className="text-[15px] leading-[23px] font-medium text-sub">{s.label}</p>
            </div>
            <p className="flex items-baseline gap-1 whitespace-nowrap">
              <span className="font-logo text-[48px] leading-[56px] font-extrabold tracking-[-1.92px] text-ink">
                {s.value}
              </span>
              <span className="text-[20px] leading-[30px] font-bold text-sub">회</span>
            </p>
          </div>
        ))}
      </section>

      <div className="flex gap-12">
        <section className="flex min-w-0 flex-1 flex-col">
          <div className="flex items-center gap-2 pb-4">
            <h2 className="text-[22px] leading-[30px] font-bold tracking-[-0.66px] text-ink">
              자주 걸리는 유형
            </h2>
            <span className="text-[14px] leading-[20px] font-medium text-faint">Top 5</span>
          </div>
          {data.ruleCounts.length === 0 && <EmptyNote>아직 걸린 유형이 없어요.</EmptyNote>}
          {data.ruleCounts.map((rule, i) => {
            const first = i === 0;
            return (
              <div
                key={rule.ruleCode}
                className="flex flex-col gap-2.5 border-t border-line py-3.5"
              >
                <div className="flex items-center gap-3">
                  <span
                    className={cn(
                      "w-5 text-[17px] leading-6 font-extrabold",
                      first ? "text-signal-red-ink" : "text-faint",
                    )}
                  >
                    {i + 1}
                  </span>
                  <p
                    className={cn(
                      "min-w-0 flex-1 text-[16px] leading-6 text-ink",
                      first ? "font-bold" : "font-medium",
                    )}
                  >
                    {topRuleTitle[rule.ruleCode] ?? rule.ruleCode}
                  </p>
                  <span
                    className={cn(
                      "text-[15px] leading-[22px] font-bold",
                      first ? "text-signal-red-ink" : "text-sub",
                    )}
                  >
                    {rule.count}회
                  </span>
                </div>
                <div className="pl-8">
                  <div className="h-2 overflow-hidden rounded-full bg-surface">
                    <div
                      className={cn("h-full rounded-full", first ? "bg-signal-red" : "bg-[#ffb3b9]")}
                      style={{ width: `${(rule.count / maxCount) * 100}%` }}
                    />
                  </div>
                </div>
              </div>
            );
          })}
        </section>

        <section className="flex min-w-0 flex-1 flex-col">
          <div className="flex items-center gap-2 pb-4">
            <h2 className="text-[22px] leading-[30px] font-bold tracking-[-0.66px] text-ink">
              최근 검수
            </h2>
            <span className="flex-1" />
            <span className="text-[14px] leading-[20px] font-medium text-signal-green-ink">
              전체 보기
            </span>
          </div>
          {data.recentChecks.length === 0 && <EmptyNote>아직 검수한 메뉴가 없어요.</EmptyNote>}
          {data.recentChecks.map((item) => (
            <RecentRow key={item.checkId} item={item} />
          ))}
        </section>
      </div>
    </div>
  );
}

function EmptyNote({ children }: { children: React.ReactNode }) {
  return <p className="border-t border-line py-6 text-[15px] leading-6 text-faint">{children}</p>;
}

function RecentRow({ item }: { item: RecentCheck }) {
  return (
    <Link
      href={`/checks/${item.checkId}`}
      className="flex items-center gap-3.5 border-t border-line py-3.5 pr-1 transition-colors hover:bg-surface-subtle"
    >
      {item.aiImageUrl ? (
        <img
          src={item.aiImageUrl}
          alt={item.menuName}
          className="h-12 w-16 shrink-0 rounded-[10px] object-cover"
        />
      ) : (
        <span
          className={cn(
            "flex h-12 w-16 shrink-0 items-center justify-center rounded-[10px]",
            thumbTone[item.overall],
          )}
        >
          <img src={bowlIcon[item.overall]} alt="" className="size-[22px]" />
        </span>
      )}
      <span className="flex min-w-0 flex-1 flex-col gap-0.5">
        <span className="text-[16px] leading-6 font-bold text-ink">{item.menuName}</span>
        <span className="text-[13px] leading-5 text-faint">
          {dateFormat.format(new Date(item.checkedAt))} · {item.note}
        </span>
      </span>
      <TrafficLight state={item.overall} />
      <ChevronRight aria-hidden className="size-5 shrink-0 text-faint" />
    </Link>
  );
}
