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

/** 규칙 코드 → 사장님이 읽기 쉬운 항목 이름 (§6.1, §6.2 · 피그마 「03 검수 결과」 카드 제목) */
export const ruleTitle: Record<RuleCode, string> = {
  IMG_EXTRA_ITEM: "실물에 없는 재료",
  IMG_COUNT: "재료 개수",
  IMG_FILL: "양·크기",
  CAP_FACT: "수치·재료가 다른 설명",
  CAP_ORIGIN: "원산지 표현",
  CAP_DISEASE: "질병 예방·치료로 오해할 표현",
  CAP_MEDICINE: "의약품으로 오해할 표현",
  CAP_HFOOD: "건강기능식품으로 오해할 표현",
  CAP_SUPERLATIVE: "근거 없는 최상급·비교",
  CAP_FAKE_REVIEW: "꾸민 후기",
  AI_VIRTUAL_EXPERT: "AI 가상 전문가",
  INFO_AI_LABEL: "연출 이미지 문구",
};

/**
 * 🟢 화면(「03-2 올려도 됩니다」)의 "확인했어요" 목록에서 쓰는 이름.
 * 건강 관련 세 규칙은 피그마처럼 "의약품·건강 표현" 한 줄로 묶어 보여 준다.
 */
export const checkedTitle: Partial<Record<RuleCode, string>> = {
  CAP_DISEASE: "의약품·건강 표현",
  CAP_MEDICINE: "의약품·건강 표현",
  CAP_HFOOD: "의약품·건강 표현",
};

/**
 * 화면에 보여 주지 않는 규칙 — 층 수(IMG_LAYER)는 서비스에서 뺐다 (#62).
 * 백엔드가 아직 보내더라도 결과 카드·「확인했어요」·판정 기준표 어디에도 띄우지 않는다.
 */
const HIDDEN_RULES: ReadonlySet<string> = new Set(["IMG_LAYER"]);
export const isShownRule = (code: string) => !HIDDEN_RULES.has(code);

/** 이미지 비교 규칙인지 (카드 부제를 "AI 사진 … → 실제 …"로 쓸지 정할 때) */
export const isImageRule = (code: RuleCode) => code.startsWith("IMG_");

export const DISCLAIMER = "법률 자문이 아닌 참고 신호입니다.";
