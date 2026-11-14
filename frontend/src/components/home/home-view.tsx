"use client";

import { ArrowRight, Check, ChevronRight } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Logo } from "@/components/logo";
import { TrafficLight } from "@/components/traffic-light";
import { cn } from "@/lib/utils";
import s from "./home.module.css";

const AI_PHOTO = "/home/ai-menu.jpg";
const REAL_PHOTO = "/home/real-menu.jpg";
const FIXED_PHOTO = "/home/ai-menu-fixed.jpg";

/** 피그마 「00 홈」 ② 단계 03 카드·③ 결과 화면과 같은 항목 */
const findings = [
  { title: "재료 개수", detail: "AI 사진 딸기 9개 → 실제 3개" },
  { title: "실물에 없는 재료", detail: "AI 사진 블루베리 → 실제 없음" },
  { title: "의약품으로 오해할 표현", detail: "메뉴 설명 “디톡스”" },
];

const steps = [
  {
    title: ["실제 메뉴 사진", "한 장이면 돼요"],
    body: ["재료나 개수를 따로 적지 않아도 돼요.", "매장에서 찍은 사진이 비교 기준이 돼요."],
  },
  {
    title: ["AI 메뉴 사진과 설명을 넣으면", "하나씩 비교해요"],
    body: ["사진은 두 번씩 읽어 재료와 개수를 세고,", "메뉴 설명 속 표현을 법령 기준표와 맞춰 봐요."],
  },
  {
    title: ["다른 곳은", "근거 조항과 함께"],
    body: ["무엇이 얼마나 다른지, 어느 조항에 걸릴 수 있는지", "항목마다 바로 보여 드려요."],
  },
];

/** 피그마 13:83 — 줄마다 [낱말, 강조] */
const statement: [string, "hr" | "hl" | null][][] = [
  [["사진 속 딸기는", null], ["아홉 개", "hr"]],
  [["실제 케이크엔", null], ["세 개", "hl"]],
  [["손님은 사진을 믿고 주문합니다.", null]],
  [["그 차이를 올리기 전에 먼저 찾아 드릴게요.", null]],
];
const statementWords = statement.map((line) =>
  line.flatMap(([text, tone]) => text.split(" ").map((word) => ({ word, tone }))),
);
const statementTotal = statementWords.reduce((n, line) => n + line.length, 0);

const stats = [
  { to: 11, unit: "개", text: "법령에 근거한 판정 규칙 (안내 2개 별도)" },
  { to: 2, unit: "개", text: "근거 법률 — 식품표시광고법, 표시광고법" },
  { to: 2, unit: "번", text: "사진마다 두 번씩 읽어서 결과를 맞춰 봐요." },
];

const chips = [
  "개수 차이 1.5배 이상",
  "실물에 없는 재료",
  "문구 속 수치 불일치",
  "의약품 오인 표현",
  "근거 없는 최상급",
];

const morphRows = [
  { label: "재료 개수", before: "딸기 9개", after: "딸기 3개" },
  { label: "실물에 없는 재료", before: "블루베리", after: "삭제" },
  { label: "의약품 표현", before: "“디톡스”", after: "삭제" },
];

const clamp = (v: number, a = 0, b = 1) => Math.min(b, Math.max(a, v));

/** 화면 00 홈 — 피그마 「00 홈 (Re:Ad)」 13:7 */
export function HomeView() {
  const pageRef = useRef<HTMLDivElement>(null);
  const showRef = useRef<HTMLDivElement>(null);
  const stepsRef = useRef<HTMLElement>(null);
  const statementRef = useRef<HTMLParagraphElement>(null);
  const morphRef = useRef<HTMLElement>(null);

  const [step, setStep] = useState(0);
  const [lit, setLit] = useState(0);
  const [fixed, setFixed] = useState(false);

  // 화면에 들어오면 나타나기 (.rv → .in)
  useEffect(() => {
    const root = pageRef.current;
    if (!root) return;
    const io = new IntersectionObserver(
      (entries) =>
        entries.forEach((e) => {
          if (!e.isIntersecting) return;
          e.target.classList.add(s.in);
          io.unobserve(e.target);
        }),
      { threshold: 0.18 },
    );
    root.querySelectorAll(`.${s.rv}`).forEach((el) => io.observe(el));
    // 첫 화면은 바로
    const id = requestAnimationFrame(() =>
      root.querySelectorAll(`[data-hero] .${s.rv}`).forEach((el) => el.classList.add(s.in)),
    );
    return () => {
      io.disconnect();
      cancelAnimationFrame(id);
    };
  }, []);

  // 스크롤에 따라 움직이는 부분 (한 프레임에 한 번)
  useEffect(() => {
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let ticking = false;
    const frame = () => {
      ticking = false;
      const vh = window.innerHeight;
      const show = showRef.current;
      if (show && !reduce) {
        const k = clamp(window.scrollY / vh, 0, 1.2);
        show.style.setProperty("--ya", `${-k * 60}px`);
        show.style.setProperty("--yb", `${-k * 20}px`);
        show.style.setProperty("--yr", `${-k * 110}px`);
      }
      const st = stepsRef.current;
      if (st) {
        const r = st.getBoundingClientRect();
        const p = clamp(-r.top / (r.height - vh), 0, 0.999);
        setStep(Math.floor(p * steps.length));
      }
      const sp = statementRef.current;
      if (sp) {
        const r = sp.getBoundingClientRect();
        const p = reduce ? 1 : clamp((vh * 0.8 - r.top) / (r.height + vh * 0.3));
        setLit(Math.round(p * statementTotal));
      }
      const m = morphRef.current;
      if (m) {
        const r = m.getBoundingClientRect();
        setFixed(-r.top / (r.height - vh) > 0.42);
      }
    };
    const onScroll = () => {
      if (!ticking) {
        ticking = true;
        requestAnimationFrame(frame);
      }
    };
    frame();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
    };
  }, []);

  let wordIndex = 0;

  return (
    <div ref={pageRef} className={s.page}>
      {/* ① 첫 화면 */}
      <section className={s.hero} data-hero>
        <div className={s.rv}>
          <Logo animated display className={s.bigLogo} />
        </div>
        <h1 className={s.rv} style={{ "--d": ".1s" } as React.CSSProperties}>
          <span className={s.line}>
            <span style={{ "--d": ".15s" } as React.CSSProperties}>AI로 만든 메뉴 사진,</span>
          </span>
          <span className={s.line}>
            <span style={{ "--d": ".27s" } as React.CSSProperties}>실제 메뉴와 같은지 보세요</span>
          </span>
        </h1>
        <p className={s.rv} style={{ "--d": ".45s" } as React.CSSProperties}>
          직접 찍은 실제 메뉴 사진과 AI로 만든 메뉴 사진을 대조해서{" "}
          <br />
          무엇이 얼마나 다른지 근거 조항과 함께 찾아 드려요.
        </p>
        <div className={cn(s.btns, s.rv)} style={{ "--d": ".58s" } as React.CSSProperties}>
          <Link href="/checks/new" className={cn(s.btn, s.primary)}>
            메뉴 사진 검수하기
            <ArrowRight className="size-[18px]" strokeWidth={2.4} />
          </Link>
          <a href="#how" className={cn(s.btn, s.ghost)}>
            어떻게 하나요
          </a>
        </div>

        <div
          ref={showRef}
          className={cn(s.show, s.rv)}
          style={{ "--d": ".75s" } as React.CSSProperties}
          aria-hidden
        >
          <div className={cn(s.ph, s.phA)}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={AI_PHOTO} alt="" />
            <span className={s.chip}>AI 메뉴 사진</span>
          </div>
          <div className={cn(s.ph, s.phB)}>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={REAL_PHOTO} alt="" />
            <span className={s.chip}>실제 메뉴</span>
          </div>
          <div className={s.result}>
            <div className={s.resultTop}>
              <TrafficLight state="RED" />
              딸기 크레이프 케이크
            </div>
            <div className={s.resultV}>
              이대로 올리기 전에 <em>4곳</em>을 고쳐 주세요
            </div>
            <div className={s.resultRows}>
              {findings.map((f, i) => (
                <div key={f.title} className={s.resultRow}>
                  <span className={s.nDot}>{i + 1}</span>
                  <div>
                    <b>{f.title}</b>
                    <span>{f.detail}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <div className={cn(s.trust, s.rv)}>
        근거로 삼는 법률
        <div className={s.trustRow}>
          <span>식품표시광고법</span>
          <span>표시광고법</span>
        </div>
      </div>

      {/* ② 단계 — 스크롤하는 동안 고정되고 01 → 02 → 03 */}
      <section ref={stepsRef} id="how" className={s.steps}>
        <div className={s.stepsPin}>
          <div className={s.copy}>
            {steps.map((st, i) => (
              <article key={i} className={cn(s.article, i === step && s.on)} aria-hidden={i !== step}>
                <div className={s.bars}>
                  {steps.map((_, k) => (
                    <i key={k} className={cn(k === i && s.barOn)} />
                  ))}
                </div>
                <div className={s.num}>{String(i + 1).padStart(2, "0")}</div>
                <h2>
                  {st.title[0]}
                  <br />
                  {st.title[1]}
                </h2>
                <p>
                  {st.body[0]}
                  <br />
                  {st.body[1]}
                </p>
              </article>
            ))}
          </div>

          <div className={s.device} aria-hidden>
            <CheckScreen on={step === 0} />
            <ComparingScreen on={step === 1} />
            <ResultScreen on={step === 2} />
          </div>
        </div>
      </section>

      {/* ③ 문장 — 스크롤을 따라 낱말이 채워진다 */}
      <section className={s.statement}>
        <p ref={statementRef}>
          {statementWords.map((line, li) => (
            <span key={li} className={s.stLine}>
              {line.map(({ word, tone }, wi) => {
                const on = wordIndex++ < lit;
                return (
                  <span key={wi} className={cn(s.w, on && s.wOn, tone && s[tone])}>
                    {word}
                    {wi < line.length - 1 ? " " : ""}
                  </span>
                );
              })}
            </span>
          ))}
        </p>
      </section>

      {/* ④ 숫자 */}
      <section className={s.numbers}>
        <h2 className={cn(s.secH, s.rv)}>
          판정은 AI가 아니라
          <br />
          정해진 기준표가 합니다
        </h2>
        <p className={cn(s.lead, s.rv)} style={{ "--d": ".1s" } as React.CSSProperties}>
          AI는 사진과 문구에서 사실만 뽑아요. 신호등 색은 법령에 근거한 기준표대로 코드가 정합니다.
        </p>
        <div className={s.grid3}>
          {stats.map((st, i) => (
            <div
              key={st.text}
              className={cn(s.stat, s.rv)}
              style={{ "--d": `${i * 0.1}s` } as React.CSSProperties}
            >
              <div className={s.n}>
                <CountUp to={st.to} />
                <small>{st.unit}</small>
              </div>
              <p>{st.text}</p>
            </div>
          ))}
        </div>
        <div className={cn(s.laws, s.rv)} style={{ "--d": ".25s" } as React.CSSProperties}>
          {chips.map((c) => (
            <span key={c}>{c}</span>
          ))}
          <span className={s.lawNew}>AI 가상 전문가 · 11.27 시행</span>
        </div>
      </section>

      {/* ⑤ 빨강 → 초록 */}
      <section ref={morphRef} className={cn(s.morph, fixed && s.ok)}>
        <div className={s.morphPin}>
          <h2 className={cn(s.secH, s.rv)}>
            고쳐서 다시 올리면,
            <br />
            신호가 바뀝니다
          </h2>
          <div className={cn(s.card, s.rv)} style={{ "--d": ".1s" } as React.CSSProperties}>
            <div className={s.head}>
              <div className={s.pics}>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={AI_PHOTO} alt="" />
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img src={FIXED_PHOTO} alt="" className={s.afterImg} />
              </div>
              <div className={s.hlBox}>
                <TrafficLight state={fixed ? "GREEN" : "RED"} />
                <div className={s.headline}>
                  <span className={s.hR}>
                    <em>4곳</em>을 고쳐 주세요
                  </span>
                  <span className={s.hG}>올려도 됩니다</span>
                </div>
              </div>
            </div>
            <ul>
              {morphRows.map((r) => (
                <li key={r.label}>
                  {r.label}
                  <span className={s.v}>
                    <span className={s.before}>{r.before}</span>
                    <span className={s.after}>{r.after}</span>
                    <span className={s.tag}>{fixed ? "맞아요" : "고쳐 주세요"}</span>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* ⑥ 마무리 */}
      <section className={s.final}>
        <div className={s.rv}>
          <Logo animated display className={s.finalLogo} />
        </div>
        <h2 className={cn(s.secH, s.rv)} style={{ "--d": ".1s" } as React.CSSProperties}>
          올리기 전에, 한 번만 확인하세요
        </h2>
        <div className={cn(s.btns, s.rv)} style={{ "--d": ".2s" } as React.CSSProperties}>
          <Link href="/checks/new" className={cn(s.btn, s.primary)}>
            지금 검수하기
            <ArrowRight className="size-[18px]" strokeWidth={2.4} />
          </Link>
          <a href="#how" className={cn(s.btn, s.ghost)}>
            어떻게 하나요
          </a>
        </div>
      </section>
    </div>
  );
}

/* ── 단계 01: 검수하기 (피그마 「01 검수하기」 13:382) ── */
function CheckScreen({ on }: { on: boolean }) {
  const typed = useTyping("딸기 크레이프 케이크");
  return (
    <div className={cn(s.screen, on && s.on)}>
      <h3>검수할 메뉴를 넣어 주세요</h3>
      <div className={s.fld}>
        <label>메뉴 이름</label>
        <div className={cn(s.inp, s.typing)}>
          <span>
            {typed}
            <span className={s.caret} />
          </span>
        </div>
      </div>
      <div className={s.fld}>
        <label>
          실제 메뉴 사진<small>매장에서 직접 찍은 사진 1장</small>
        </label>
        <div className={s.photo}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={REAL_PHOTO} alt="" />
          <div>
            real-cake.jpg<small>1.2MB</small>
          </div>
          <span className={s.chg}>바꾸기</span>
        </div>
      </div>
      <div className={s.fld}>
        <label>AI 메뉴 사진</label>
        <div className={s.photo}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={AI_PHOTO} alt="" />
          <div>
            ai-cake.jpg<small>2.4MB</small>
          </div>
          <span className={s.chg}>바꾸기</span>
        </div>
      </div>
      <div className={cn(s.fld, s.hideNarrow)}>
        <label>메뉴 설명</label>
        <div className={s.textarea}>
          인생 케이크 등장! 딸기를 듬뿍 올렸어요. 혈당 걱정 없는 디톡스 디저트로 딱이에요.
        </div>
      </div>
      <div className={s.sBtn}>검수하기</div>
    </div>
  );
}

function useTyping(word: string) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      const id = requestAnimationFrame(() => setCount(word.length));
      return () => cancelAnimationFrame(id);
    }
    let i = 0;
    let timer: ReturnType<typeof setTimeout>;
    const tick = () => {
      i = i > word.length + 10 ? 0 : i + 1;
      setCount(Math.min(i, word.length));
      timer = setTimeout(tick, i === 0 ? 500 : 120);
    };
    timer = setTimeout(tick, 400);
    return () => clearTimeout(timer);
  }, [word]);
  return word.slice(0, count);
}

/* ── 단계 02: 비교하고 있어요 (노란불은 여기서만) — 피그마 「02 분석 중」 13:440 ── */
function ComparingScreen({ on }: { on: boolean }) {
  const tasks = [
    { label: "두 사진에서 재료와 개수 읽기", state: s.done },
    { label: "메뉴 설명에서 주장 찾기", state: s.done },
    { label: "법령 기준표와 대조하기", state: s.now },
    { label: "고친 문구와 프롬프트 만들기", state: s.todo },
  ];
  return (
    <div className={cn(s.screen, on && s.on)}>
      <h3>
        <TrafficLight state="COMPARING" />
        비교하고 있어요
      </h3>
      <div className={s.thumbs}>
        <div>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={AI_PHOTO} alt="" />
          <span className={s.scan} />
          <span className={s.chip}>AI 메뉴 사진</span>
        </div>
        <div>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={REAL_PHOTO} alt="" />
          <span className={s.chip}>실제 메뉴</span>
        </div>
      </div>
      <div className={s.progress}>
        <b />
      </div>
      <div>
        {tasks.map((t) => (
          <div key={t.label} className={cn(s.task, t.state)}>
            <span className={s.st}>
              {t.state === s.done && <Check className="size-3.5" strokeWidth={3} />}
            </span>
            {t.label}
          </div>
        ))}
      </div>
    </div>
  );
}

/* ── 단계 03: 결과 (피그마 device 13:48) ── */
function ResultScreen({ on }: { on: boolean }) {
  return (
    <div className={cn(s.screen, on && s.on)}>
      <div className={s.meta}>
        <TrafficLight state="RED" />
        딸기 크레이프 케이크
      </div>
      <div className={s.verdict}>
        이대로 올리기 전에
        <br />
        <em>4곳</em>을 고쳐 주세요
      </div>
      <div className={s.thumbs}>
        <div>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={AI_PHOTO} alt="" />
          <span className={s.chip}>AI 메뉴 사진</span>
        </div>
        <div>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={REAL_PHOTO} alt="" />
          <span className={s.chip}>실제 메뉴</span>
        </div>
      </div>
      {findings.map((f, i) => (
        <div key={f.title} className={s.find}>
          <span className={s.nDot}>{i + 1}</span>
          <div>
            <b>{f.title}</b>
            <span>{f.detail}</span>
          </div>
          <ChevronRight className={cn("size-[18px]", s.chev)} />
        </div>
      ))}
    </div>
  );
}

function CountUp({ to }: { to: number }) {
  const ref = useRef<HTMLSpanElement>(null);
  const [value, setValue] = useState(0);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let frame = 0;
    const io = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;
        io.disconnect();
        const start = performance.now();
        const duration = reduce ? 0 : 1400;
        const step = (t: number) => {
          const p = duration ? Math.min(1, (t - start) / duration) : 1;
          setValue(Math.round(to * (1 - Math.pow(1 - p, 4))));
          if (p < 1) frame = requestAnimationFrame(step);
        };
        frame = requestAnimationFrame(step);
      },
      { threshold: 0.4 },
    );
    io.observe(el);
    return () => {
      io.disconnect();
      cancelAnimationFrame(frame);
    };
  }, [to]);
  return <span ref={ref}>{value}</span>;
}
