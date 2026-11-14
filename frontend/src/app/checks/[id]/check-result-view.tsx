"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { Check, ChevronDown, ChevronRight, Info } from "lucide-react";
import { toast } from "sonner";
import { TrafficLight } from "@/components/traffic-light";
import { Button, buttonVariants } from "@/components/ui/button";
import { useCheck, useRules } from "@/lib/api/queries";
import { checkedTitle, isImageRule, ruleTitle } from "@/lib/severity";
import type { CheckResult, Finding, Rule, RuleCode } from "@/lib/types";
import { cn } from "@/lib/utils";
import { CheckResultSkeleton } from "./check-result-skeleton";

/*
 * 화면 3 — 검수 결과 (OLLYEODO_MVP.md §11, 이슈 #16)
 * 피그마 「03 검수 결과 — 고쳐 주세요」(13:169) · 「03-2 올려도 됩니다」(13:485) 그대로.
 * 결과는 🔴·🟢 두 가지뿐이다 (§6, #33). 판정은 백엔드 코드가 하고 화면은 받은 값만 보여 준다.
 * 문구는 단정하지 않고 "이 조항에 걸릴 수 있는 차이"로 쓴다 (AGENTS.md).
 */

/** 본문 단 — 피그마 600px */
function Col({ className, children }: { className?: string; children: React.ReactNode }) {
  return <div className={cn("mx-auto w-full max-w-[632px] px-4", className)}>{children}</div>;
}

/** 섹션 사이 구분선 — 본문 폭에 맞춘 얇은 1px 선 (피그마의 12px 회색 띠 대신) */
const Band = () => (
  <Col>
    <hr className="border-line" />
  </Col>
);

/** "2026-11-14" → "11월 14일" */
function koDate(iso: string) {
  const [, m, d] = iso.split("-").map(Number);
  return m && d ? `${m}월 ${d}일` : iso;
}

/** asOf 기준으로 아직 시행 전인 규칙인지 (§6.3 4번) */
function notYetEffective(rule: Rule | undefined, asOf: string) {
  return !!rule && rule.effectiveFrom > asOf;
}

/** 클립보드 쓰기. 권한이 막힌 브라우저(앱 안 미리보기 등)에서는 예전 방식으로 한 번 더 시도한다 */
async function writeClipboard(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    const ok = document.execCommand("copy");
    area.remove();
    return ok;
  }
}

/** 복사 버튼 — 누르면 잠깐 "복사했어요"로 바뀐다 (피그마 13:557) */
function CopyButton({ text, what }: { text: string; what: string }) {
  const [copied, setCopied] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);
  useEffect(() => () => clearTimeout(timer.current), []);

  async function onCopy() {
    if (!(await writeClipboard(text))) {
      toast.error("복사하지 못했어요. 직접 선택해서 복사해 주세요.");
      return;
    }
    setCopied(true);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => setCopied(false), 2000);
  }

  return (
    <button
      type="button"
      onClick={onCopy}
      className="inline-flex items-center gap-1 rounded-md text-sm leading-5 font-bold text-signal-green-ink outline-none hover:underline focus-visible:ring-2 focus-visible:ring-ring"
    >
      {copied && <Check aria-hidden className="size-[15px]" strokeWidth={2.5} />}
      <span aria-live="polite">{copied ? "복사했어요" : "복사하기"}</span>
      <span className="sr-only"> — {what}</span>
    </button>
  );
}

/** 회색 글상자 + 오른쪽 아래 복사 (피그마 13:270) */
function CopyBox({ text, what, mono }: { text: string; what: string; mono?: boolean }) {
  return (
    <div className="flex w-full flex-col gap-3 rounded-2xl bg-surface px-5 pt-[18px] pb-3.5">
      <p
        className={cn(
          "whitespace-pre-line text-ink",
          mono ? "text-[15px] leading-[25px]" : "text-[17px] leading-7",
        )}
      >
        {text}
      </p>
      <div className="flex justify-end">
        <CopyButton text={text} what={what} />
      </div>
    </div>
  );
}

/** 상단 — 신호등 · 큰 제목 · AI 사진과 실제 사진 나란히 */
function Verdict({ data, redCount }: { data: CheckResult; redCount: number }) {
  const passed = data.overall === "GREEN";
  const photos = [
    { src: data.aiImageUrl, label: passed ? "고친 AI 메뉴 사진" : "AI 메뉴 사진" },
    { src: data.realImageUrl, label: "실제 메뉴" },
  ].filter((p): p is { src: string; label: string } => !!p.src);

  return (
    <section className="pt-12 pb-10">
      <Col>
        <TrafficLight state={passed ? "GREEN" : "RED"} />
        <h1 className="mt-[22px] text-[28px] leading-[38px] font-bold tracking-[-0.035em] text-ink sm:text-[34px] sm:leading-[46px]">
          {passed ? (
            "올려도 됩니다"
          ) : (
            <>
              이대로 올리기 전에
              <br />
              <span className="text-signal-red-ink">{redCount}곳</span>을 고쳐 주세요
            </>
          )}
        </h1>
        <p className="mt-3 text-base leading-6 text-sub">
          {passed
            ? "실제 메뉴 사진과 다른 곳을 찾지 못했어요."
            : `${data.menuName ? `${data.menuName} · ` : ""}${koDate(data.asOf)} 현행 법령으로 검수했어요`}
        </p>
        {photos.length > 0 && (
          <div className="mt-7 grid grid-cols-2 gap-2.5">
            {photos.map((p) => (
              <figure
                key={p.label}
                className="relative aspect-[295/221] overflow-hidden rounded-2xl bg-surface"
              >
                {/* 서명 URL(외부 주소)도 그대로 보여 주려고 next/image 대신 img 를 쓴다 */}
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={p.src} alt={p.label} className="size-full object-cover" />
                <figcaption className="absolute top-3 left-3 rounded-lg bg-ink/72 px-[9px] py-1 text-xs leading-[18px] font-bold text-white">
                  {p.label}
                </figcaption>
              </figure>
            ))}
          </div>
        )}
      </Col>
    </section>
  );
}

/** 카드 부제 — 이미지 규칙은 "AI 사진 딸기 9개 → 실제 3개", 문구 규칙은 메뉴 설명 “디톡스” */
function findingSummary(f: Finding) {
  if (isImageRule(f.ruleCode)) {
    if (!f.aiValue) return f.evidence;
    // 실제 값이 같은 재료 이름으로 시작하면 이름은 한 번만 쓴다 (딸기 9개 → 실제 3개)
    const [aiName] = f.aiValue.split(" ");
    const real =
      f.realValue && f.realValue.startsWith(`${aiName} `)
        ? f.realValue.slice(aiName.length + 1)
        : f.realValue;
    return `AI 사진 ${f.aiValue} → 실제 ${real ?? "확인 안 됨"}`;
  }
  return f.aiValue ? `메뉴 설명 “${f.aiValue}”` : f.evidence;
}

/** 🔴 항목 한 줄 + 펼치면 사유·근거 조항·원문 링크 (피그마 13:211) */
function FindingRow({
  finding,
  index,
  rule,
  asOf,
  open,
  onToggle,
}: {
  finding: Finding;
  index: number;
  rule?: Rule;
  asOf: string;
  open: boolean;
  onToggle: () => void;
}) {
  const panelId = `finding-${index}`;
  const lawRef = finding.lawRef ?? rule?.lawRef;
  // §5.1: AI 가상 전문가 처분 기준은 "시행 예정"으로만 알린다
  const sanctionPending = finding.ruleCode === "AI_VIRTUAL_EXPERT";

  return (
    <li>
      <button
        type="button"
        aria-expanded={open}
        aria-controls={panelId}
        onClick={onToggle}
        className="flex w-full items-center gap-4 rounded-xl py-4 text-left outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-signal-red-soft text-base leading-5 font-bold text-signal-red-ink">
          {index + 1}
        </span>
        <span className="flex min-w-0 flex-1 flex-col gap-0.5">
          <span className="text-[17px] leading-[25px] font-bold text-ink">
            {ruleTitle[finding.ruleCode]}
          </span>
          <span className="text-[15px] leading-[22px] text-sub">{findingSummary(finding)}</span>
        </span>
        <ChevronDown
          aria-hidden
          className={cn(
            "size-5 shrink-0 text-faint transition-transform duration-200",
            open && "rotate-180",
          )}
        />
      </button>
      {open && (
        <div id={panelId} className="pb-3 pl-14">
          <div className="flex flex-col gap-1 rounded-[14px] bg-surface px-[18px] py-4">
            {finding.evidence && (
              <p className="text-base leading-6 font-medium text-ink">{finding.evidence}</p>
            )}
            {lawRef && (
              <p className="text-sm leading-[22px] text-sub">
                {lawRef}에 걸릴 수 있는 차이예요.
              </p>
            )}
            {sanctionPending && (
              <p className="text-sm leading-[22px] text-sub">
                영업정지 처분 기준은 시행 예정이에요.
              </p>
            )}
            {rule?.sourceUrl && (
              <a
                href={rule.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex w-fit items-center gap-0.5 pt-1.5 text-sm leading-5 font-bold text-signal-green-ink hover:underline"
              >
                법령 원문 보기
                <ChevronRight aria-hidden className="size-[15px]" strokeWidth={2.5} />
                <span className="sr-only">(새 창)</span>
              </a>
            )}
            {notYetEffective(rule, asOf) && (
              <span className="mt-1 w-fit rounded-full bg-white px-2.5 py-1 text-xs font-bold text-sub">
                {koDate(rule!.effectiveFrom)}부터 적용
              </span>
            )}
          </div>
        </div>
      )}
    </li>
  );
}

/** "판정 기준표" — §6 규칙을 펼쳐 보여 준다 (GET /api/rules) */
function RulesTable({ rules, asOf }: { rules: Rule[]; asOf: string }) {
  return (
    <div className="mb-2 overflow-x-auto rounded-[14px] border border-line">
      <table className="w-full min-w-[480px] text-left text-[13px] leading-5">
        <thead className="bg-surface-subtle text-faint">
          <tr>
            <th className="px-3 py-2 font-medium">항목</th>
            <th className="px-3 py-2 font-medium">근거 조항</th>
            <th className="px-3 py-2 font-medium">시행</th>
          </tr>
        </thead>
        <tbody className="text-sub">
          {rules.map((r) => (
            <tr key={r.code} className="border-t border-line">
              <td className="px-3 py-2 text-ink">{ruleTitle[r.code]}</td>
              <td className="px-3 py-2">{r.lawRef}</td>
              <td className="px-3 py-2 whitespace-nowrap">
                {notYetEffective(r, asOf) ? `${koDate(r.effectiveFrom)}부터` : "시행 중"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * "통과지만 한 번 확인해 보세요" — 🟢 이지만 직접 한 번 봐야 할 항목 (checkOnce).
 * 피그마 RED 47:158 · GREEN 47:168. 판정은 🟢 그대로라 노란색을 쓰지 않고 회색 "!" 로 보여 준다.
 */
function CheckOnce({
  findings,
  rulesByCode,
  asOf,
}: {
  findings: Finding[];
  rulesByCode: Map<RuleCode, Rule>;
  asOf: string;
}) {
  if (findings.length === 0) return null;
  return (
    <div className="pt-6">
      <h3 className="text-[17px] leading-[25px] font-bold text-ink">통과지만 한 번 확인해 보세요</h3>
      <p className="mt-0.5 text-sm leading-[21px] text-faint">
        고칠 곳은 아니에요. 올리기 전에 직접 한 번만 봐 주세요.
      </p>
      <ul className="pt-1">
        {findings.map((f, i) => {
          const rule = rulesByCode.get(f.ruleCode);
          return (
            <li key={`${f.ruleCode}-${i}`} className="flex items-center gap-4 py-3.5">
              <span
                aria-hidden
                className="flex size-10 shrink-0 items-center justify-center rounded-full bg-surface text-base leading-5 font-bold text-sub"
              >
                !
              </span>
              <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                <span className="text-[17px] leading-[25px] font-bold text-ink">
                  {ruleTitle[f.ruleCode]}
                </span>
                {f.evidence && (
                  <span className="text-[15px] leading-[22px] text-sub">{f.evidence}</span>
                )}
                {notYetEffective(rule, asOf) && (
                  <span className="mt-1 w-fit rounded-full bg-surface px-2.5 py-1 text-xs font-bold text-sub">
                    {koDate(rule!.effectiveFrom)}부터 금지
                  </span>
                )}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/** 🔴 화면 — 고쳐야 할 곳 */
function Findings({
  data,
  reds,
  rulesByCode,
}: {
  data: CheckResult;
  reds: Finding[];
  rulesByCode: Map<RuleCode, Rule>;
}) {
  const [openIndex, setOpenIndex] = useState<number | null>(0);
  const [showRules, setShowRules] = useState(false);
  const rules = [...rulesByCode.values()];

  // 🟢 중 직접 한 번 봐야 할 항목은 따로, 나머지는 판정에 영향 없는 안내 (INFO_AI_LABEL 등)
  const greens = data.findings.filter((f) => f.severity === "GREEN");
  const checkOnce = greens.filter((f) => f.checkOnce);
  const notes = greens.filter((f) => !f.checkOnce && f.evidence);

  return (
    <section className="py-10">
      <Col>
        <div className="flex items-center justify-between pb-2">
          <h2 className="flex gap-1.5 text-[22px] leading-[30px] font-bold">
            <span className="tracking-[-0.02em] text-ink">고쳐야 할 곳</span>
            <span className="text-signal-red-ink">{reds.length}</span>
          </h2>
          {rules.length > 0 && (
            <button
              type="button"
              aria-expanded={showRules}
              onClick={() => setShowRules((v) => !v)}
              className="inline-flex items-center gap-0.5 rounded-md text-sm leading-[21px] text-faint outline-none hover:text-sub focus-visible:ring-2 focus-visible:ring-ring"
            >
              판정 기준표
              <ChevronRight
                aria-hidden
                className={cn("size-4 transition-transform", showRules && "rotate-90")}
              />
            </button>
          )}
        </div>
        {showRules && <RulesTable rules={rules} asOf={data.asOf} />}
        <ol>
          {reds.map((f, i) => (
            <FindingRow
              key={`${f.ruleCode}-${i}`}
              finding={f}
              index={i}
              rule={rulesByCode.get(f.ruleCode)}
              asOf={data.asOf}
              open={openIndex === i}
              onToggle={() => setOpenIndex(openIndex === i ? null : i)}
            />
          ))}
        </ol>
        <CheckOnce findings={checkOnce} rulesByCode={rulesByCode} asOf={data.asOf} />
        {notes.map((f, i) => {
          const rule = rulesByCode.get(f.ruleCode);
          const upcoming = notYetEffective(rule, data.asOf);
          return (
            <p key={`${f.ruleCode}-${i}`} className="flex gap-2 pt-5 text-[13px] leading-5 text-faint">
              <Info aria-hidden className="mt-0.5 size-4 shrink-0" />
              <span>
                {upcoming && (
                  <span className="mr-1.5 rounded-full bg-signal-red-soft px-2 py-0.5 text-xs font-bold text-signal-red-ink">
                    {koDate(rule!.effectiveFrom)}부터 금지
                  </span>
                )}
                {f.ruleCode !== "INFO_AI_LABEL" && `${ruleTitle[f.ruleCode]} — `}
                {f.evidence}
              </span>
            </p>
          );
        })}
      </Col>
    </section>
  );
}

/** 🔴 화면 — 고친 문구 + 다시 만들 때 쓸 프롬프트 (§10) */
function Fix({ data }: { data: CheckResult }) {
  const [lang, setLang] = useState<"ko" | "en">("ko");
  const prompt = data.regenPrompt[lang];

  return (
    <section className="pt-10 pb-7">
      <Col>
        <h2 className="text-[22px] leading-[30px] font-bold tracking-[-0.02em] text-ink">
          이렇게 고쳐서 올리세요
        </h2>
        <p className="mt-1.5 text-[15px] leading-[23px] text-sub">
          고친 문구는 그대로 붙여 넣고, 이미지는 프롬프트로 다시 만들어 보세요.
        </p>

        <h3 className="mt-6 pb-2.5 text-[15px] leading-[23px] font-bold text-sub">고친 문구</h3>
        <CopyBox text={data.captionFix} what="고친 문구" />

        {(data.regenPrompt.ko || data.regenPrompt.en) && (
          <>
            <div className="mt-6 flex items-center justify-between pb-2.5">
              <h3 className="text-[15px] leading-[23px] font-bold text-sub">
                다시 만들 때 쓸 프롬프트
              </h3>
              <div role="group" aria-label="프롬프트 언어" className="flex rounded-[10px] bg-surface p-[3px]">
                {(
                  [
                    ["ko", "한국어"],
                    ["en", "English"],
                  ] as const
                ).map(([value, label]) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={lang === value}
                    onClick={() => setLang(value)}
                    className={cn(
                      "rounded-lg px-3.5 py-1 text-[13px] leading-5 outline-none focus-visible:ring-2 focus-visible:ring-ring",
                      lang === value
                        ? "bg-white font-bold text-ink shadow-[0_1px_3px_rgba(0,0,0,0.08)]"
                        : "font-medium text-faint hover:text-sub",
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
            <CopyBox key={lang} text={prompt} what="프롬프트" mono />
          </>
        )}

        <Link
          href="/checks/new"
          className={cn(buttonVariants({ size: "cta" }), "mt-6 w-full bg-signal-green-strong hover:bg-signal-green-strong/90")}
        >
          고친 사진으로 다시 검수
        </Link>
        <div className="h-10" />
      </Col>
    </section>
  );
}

/** 🟢 화면 — 확인했어요 (피그마 13:517) */
function Checked({
  findings,
  rulesByCode,
  asOf,
}: {
  findings: Finding[];
  rulesByCode: Map<RuleCode, Rule>;
  asOf: string;
}) {
  const greens = findings.filter((f) => f.severity === "GREEN");
  const checkOnce = greens.filter((f) => f.checkOnce);
  // 같은 이름으로 묶이는 규칙(의약품·건강 표현)은 한 줄만
  const seen = new Set<string>();
  const rows = greens
    .filter((f) => !f.checkOnce)
    .map((f) => ({ title: checkedTitle[f.ruleCode] ?? ruleTitle[f.ruleCode], text: f.evidence }))
    .filter((r) => (seen.has(r.title) ? false : (seen.add(r.title), true)));

  if (rows.length === 0 && checkOnce.length === 0) return null;
  return (
    <>
      <Band />
      <section className="py-10">
        <Col>
          <h2 className="pb-2 text-[22px] leading-[30px] font-bold tracking-[-0.02em] text-ink">
            확인했어요
          </h2>
          <ul>
            {rows.map((r) => (
              <li key={r.title} className="flex items-center gap-4 py-3.5">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-signal-green-soft text-signal-green-ink">
                  <Check aria-hidden className="size-[18px]" strokeWidth={2.5} />
                </span>
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="text-[17px] leading-[25px] font-bold text-ink">{r.title}</span>
                  {r.text && <span className="text-[15px] leading-[22px] text-sub">{r.text}</span>}
                </span>
              </li>
            ))}
          </ul>
          <CheckOnce findings={checkOnce} rulesByCode={rulesByCode} asOf={asOf} />
        </Col>
      </section>
    </>
  );
}

/** 🟢 화면 — 이 문구 그대로 올리면 돼요 */
function PassedCaption({ data }: { data: CheckResult }) {
  return (
    <section className="pt-10 pb-7">
      <Col>
        <h2 className="text-[22px] leading-[30px] font-bold tracking-[-0.02em] text-ink">
          이 문구 그대로 올리면 돼요
        </h2>
        <div className="mt-5">
          <CopyBox text={data.captionFix} what="문구" />
        </div>
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
            다른 메뉴 검수
          </Link>
        </div>
        <div className="h-10" />
      </Col>
    </section>
  );
}

export function CheckResultView() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { data, isPending, isError, error, refetch } = useCheck(id);
  const { data: rules } = useRules();

  if (!Number.isFinite(id)) {
    return (
      <Col className="py-16">
        <p className="text-sub">잘못된 검수 번호예요.</p>
      </Col>
    );
  }
  if (isPending) return <CheckResultSkeleton />;
  if (isError) {
    return (
      <Col className="space-y-3 py-16 text-center">
        <p className="text-sub">결과를 불러오지 못했어요. ({error.message})</p>
        <Button variant="outline" onClick={() => refetch()}>
          다시 시도
        </Button>
      </Col>
    );
  }

  const rulesByCode = new Map((rules ?? []).map((r) => [r.code, r]));
  const reds = data.findings.filter((f) => f.severity === "RED");
  // 종합 신호는 백엔드 값(overall)을 그대로 쓴다 — 화면에서 다시 판정하지 않는다
  const passed = data.overall === "GREEN";

  return (
    <div className="flex flex-col">
      <Verdict data={data} redCount={reds.length} />
      {passed ? (
        <>
          <Checked findings={data.findings} rulesByCode={rulesByCode} asOf={data.asOf} />
          <Band />
          <PassedCaption data={data} />
        </>
      ) : (
        <>
          <Band />
          <Findings data={data} reds={reds} rulesByCode={rulesByCode} />
          <Band />
          <Fix data={data} />
        </>
      )}
    </div>
  );
}
