import type { CheckResult, Menu, Rule } from "@/lib/types";

/** OLLYEODO_MVP.md §9.1 응답 예시 그대로 */
export const mockCheckResult: CheckResult = {
  checkId: 12,
  asOf: "2026-11-14",
  overall: "RED",
  findings: [
    {
      ruleCode: "IMG_COUNT",
      severity: "RED",
      aiValue: "딸기 9개",
      realValue: "딸기 4개",
      evidence: "실물의 2.25배",
      lawRef: "식품표시광고법 제8조①4호",
    },
    {
      ruleCode: "CAP_MEDICINE",
      severity: "RED",
      aiValue: "디톡스",
      evidence: "의약품 오인 표현",
      lawRef: "식품표시광고법 제8조①2호",
    },
  ],
  captionFix: "8겹 크레이프에 딸기를 올린 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: {
    ko: "딸기 크레이프 케이크 1조각, ...",
    en: "One slice of strawberry crepe cake, ...",
  },
};

export const mockMenus: Menu[] = [
  {
    id: 1,
    name: "딸기 크레이프 케이크",
    facts: {
      name: "딸기 크레이프 케이크",
      ingredients: [{ name: "딸기", count: 4 }],
      layers: 8,
      origin: "딸기: 국내산",
    },
    createdAt: "2026-11-14T18:00:00+09:00",
  },
];

export const mockRules: Rule[] = [
  {
    code: "AI_VIRTUAL_EXPERT",
    title: "AI 가상 전문가",
    lawRef: "식품표시광고법 제8조①11호",
    effectiveFrom: "2026-11-27",
    effectiveTo: null,
    severity: "RED",
    preEffectiveSeverity: "YELLOW",
  },
];
