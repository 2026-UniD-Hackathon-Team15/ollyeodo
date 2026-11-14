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
      realValue: "딸기 3개",
      evidence: "실물보다 3배 많아 보여요.",
      lawRef: "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호",
    },
    {
      ruleCode: "IMG_EXTRA_ITEM",
      severity: "RED",
      aiValue: "블루베리",
      realValue: "없음",
      evidence: "실제 메뉴 사진에 없는 재료예요.",
      lawRef: "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호",
    },
    {
      ruleCode: "CAP_HFOOD",
      severity: "RED",
      aiValue: "혈당 걱정 없는",
      evidence: "건강기능식품으로 오해할 표현",
      lawRef: "식품표시광고법 제8조①3호",
    },
    {
      ruleCode: "CAP_MEDICINE",
      severity: "RED",
      aiValue: "디톡스",
      evidence: "의약품으로 오해할 표현",
      lawRef: "식품표시광고법 제8조①2호",
    },
  ],
  captionFix: "딸기 3개를 올린 크레이프 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: {
    ko: "딸기 크레이프 케이크 1조각, 흰 원형 접시, 위에서 45도, 자연광.\n크레이프 사이 생크림과 딸기 조각, 윗면 딸기 3개. 실제 매장 사진과 같은 크기와 양.\n넣지 말 것: 블루베리, 딸기 4개 이상, 접시를 넘치는 크림, 글자.",
    en: "One slice of strawberry crepe cake on a white round plate, 45-degree top view, natural light.\nWhipped cream and strawberry pieces between crepe layers, exactly 3 strawberries on top. Same size and amount as the real store photo.\nAvoid: blueberries, 4 or more strawberries, cream overflowing the plate, text.",
  },
};

export const mockMenus: Menu[] = [
  {
    id: 1,
    name: "딸기 크레이프 케이크",
    facts: { name: "딸기 크레이프 케이크" },
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
