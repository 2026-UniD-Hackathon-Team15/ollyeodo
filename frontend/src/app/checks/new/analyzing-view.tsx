"use client";

/* eslint-disable @next/next/no-img-element */
// 검수하기 화면에서 고른 사진(blob URL)을 그대로 보여줘야 해서 next/image 대신 img를 쓴다.

import { useEffect, useState } from "react";
import { TrafficLight } from "@/components/traffic-light";
import { cn } from "@/lib/utils";
import styles from "./analyzing-view.module.css";

// 단계별 이름과 목업 소요 시간(ms). API가 진행 상황을 주지 않으므로 마지막 단계에서 응답을 기다린다.
const STEPS = [
  { label: "두 사진에서 재료와 개수 읽기", duration: 1600 },
  { label: "메뉴 설명에서 주장 찾기", duration: 1200 },
  { label: "법령 기준표와 대조하기", duration: 1600 },
  { label: "고친 문구와 프롬프트 만들기", duration: 1200 },
];

function CheckIcon() {
  return (
    <svg
      width="15"
      height="15"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#fff"
      strokeWidth="3"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20 6 9 17l-5-5" />
    </svg>
  );
}

function delay(index: number) {
  return { animationDelay: `${0.1 + index * 0.06}s` };
}

/** 화면 02 분석 중 — 피그마 「02 분석 중」 13:440 */
export function AnalyzingView({ image }: { image: string }) {
  const [current, setCurrent] = useState(0);
  const last = STEPS.length - 1;

  useEffect(() => {
    if (current >= last) return;
    const timer = setTimeout(() => setCurrent((c) => c + 1), STEPS[current].duration);
    return () => clearTimeout(timer);
  }, [current, last]);

  const percent = ((current + 0.5) / STEPS.length) * 100;

  return (
    <div className={styles.page}>
      <div className={styles.col} aria-live="polite">
        <div className={styles.reveal} style={delay(0)}>
          <TrafficLight state="COMPARING" />
        </div>

        <h1 className={cn(styles.title, styles.reveal)} style={{ ...delay(1), whiteSpace: "pre-line" }}>
          {"메뉴 사진을 실제 메뉴와\n비교하고 있어요"}
        </h1>
        <p className={cn(styles.lead, styles.reveal)} style={delay(2)}>
          사진을 세 번씩 읽어서 비교하고 있어요. 이 화면을 닫지 말아 주세요.
        </p>

        <div className={cn(styles.thumb, styles.reveal)} style={delay(3)}>
          <img src={image} alt="검수 중인 AI 메뉴 사진" />
          <span className={styles.scan} />
        </div>

        <div
          className={cn(styles.progress, styles.reveal)}
          style={delay(4)}
          role="progressbar"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(percent)}
        >
          <div className={styles.progressBar} style={{ width: `${percent}%` }} />
        </div>

        <ol className={cn(styles.steps, styles.reveal)} style={delay(5)}>
          {STEPS.map((step, index) => {
            const isDone = index < current;
            const isCurrent = index === current;
            return (
              <li
                key={step.label}
                className={cn(styles.step, isDone && styles.stepDone, isCurrent && styles.stepCurrent)}
              >
                <span className={styles.stepIcon}>
                  {isDone && <CheckIcon />}
                  {isCurrent && <span className={styles.dot} />}
                </span>
                {step.label}
              </li>
            );
          })}
        </ol>

        <p className={cn(styles.note, styles.reveal)} style={delay(6)}>
          판정은 AI가 아니라 정해진 기준표대로 코드가 합니다. AI는 사진과 문구에서 사실만 뽑아요.
        </p>
      </div>
    </div>
  );
}
