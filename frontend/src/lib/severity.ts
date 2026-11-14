import type { RuleCode, Severity } from "./types";

/**
 * 신호등 표시 정보 — OLLYEODO_MVP.md §6 정의.
 * 화면 문구는 단정 표현 대신 "이 조항에 걸릴 수 있는 차이"처럼 참고 신호로 쓴다 (AGENTS.md).
 */
export const severityMeta: Record<
  Severity,
  { emoji: string; label: string; description: string; rank: number }
> = {
  GREEN: {
    emoji: "🟢",
    label: "올려도 돼요",
    description: "그대로 올려도 되는 신호예요.",
    rank: 0,
  },
  YELLOW: {
    emoji: "🟡",
    label: "확인해 주세요",
    description: "고치면 좋거나 직접 확인이 필요한 항목이에요.",
    rank: 1,
  },
  RED: {
    emoji: "🔴",
    label: "고쳐 주세요",
    description: "실물과 다르거나 민원 위험이 있는 신호예요.",
    rank: 2,
  },
};

/** 규칙 코드 → 사장님이 읽기 쉬운 항목 이름 (§6.1, §6.2) */
export const ruleTitle: Record<RuleCode, string> = {
  IMG_EXTRA_ITEM: "없는 재료",
  IMG_COUNT: "개수",
  IMG_FILL: "양·크기",
  IMG_LAYER: "층 수·구성",
  CAP_FACT: "수치·재료 불일치",
  CAP_ORIGIN: "원산지 불일치",
  CAP_DISEASE: "질병 예방·치료 표현",
  CAP_MEDICINE: "의약품 오인 표현",
  CAP_HFOOD: "건강기능식품 오인 표현",
  CAP_SUPERLATIVE: "근거 없는 최상급·비교",
  CAP_FAKE_REVIEW: "꾸민 후기",
  AI_VIRTUAL_EXPERT: "AI 가상 전문가",
  INFO_AI_LABEL: "AI 생성 안내",
};

export const DISCLAIMER = "법률 자문이 아닌 참고 신호입니다.";
