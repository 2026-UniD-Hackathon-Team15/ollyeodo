import type { CheckResult, Menu, Rule } from "@/lib/types";

/**
 * 🔴 결과 목업 — OLLYEODO_MVP.md §9.1 응답 모양 + 피그마 「03 검수 결과 — 고쳐 주세요」(13:169) 내용.
 * GET /api/checks/12 (목업)
 */
export const mockCheckResult: CheckResult = {
  checkId: 12,
  asOf: "2026-11-14",
  overall: "RED",
  menuName: "딸기 크레이프 케이크",
  aiImageUrl: "/home/ai-menu.jpg",
  realImageUrl: "/home/real-menu.jpg",
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
      evidence: "건강기능식품으로 오해할 수 있는 표현이에요.",
      lawRef: "식품표시광고법 제8조①3호",
    },
    {
      ruleCode: "CAP_MEDICINE",
      severity: "RED",
      aiValue: "디톡스",
      evidence: "의약품으로 오해할 수 있는 표현이에요.",
      lawRef: "식품표시광고법 제8조①2호",
    },
    {
      // 피그마 47:158 — 🟢 통과지만 한 번 확인 (기준 1.2~1.5배는 팀 합의 후 §6 추가)
      ruleCode: "IMG_FILL",
      severity: "GREEN",
      checkOnce: true,
      aiValue: "크림 1.3배",
      evidence: "AI 사진 크림 양이 실물의 1.3배예요. 기준(1.5배) 아래라 통과예요.",
    },
    {
      ruleCode: "INFO_AI_LABEL",
      severity: "GREEN",
      evidence:
        "AI로 만든 사진이라면 “연출된 이미지입니다”를 함께 적어 주세요. 판정과는 상관없는 안내예요.",
    },
  ],
  captionFix: "딸기 3개를 올린 크레이프 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: {
    ko: "딸기 크레이프 케이크 1조각, 흰 원형 접시, 위에서 45도, 자연광.\n크레이프 사이 생크림과 딸기 조각, 윗면 딸기 3개. 실제 매장 사진과 같은 크기와 양.\n넣지 말 것: 블루베리, 딸기 4개 이상, 접시를 넘치는 크림, 글자.",
    en: "One slice of strawberry crepe cake on a white round plate, 45-degree top view, natural light.\nWhipped cream and strawberry pieces between crepe layers, exactly 3 strawberries on top. Same size and amount as the real store photo.\nAvoid: blueberries, 4 or more strawberries, cream overflowing the plate, text.",
  },
};

/**
 * 🟢 결과 목업 — 피그마 「03-2 검수 결과 — 올려도 됩니다」(13:485).
 * 재생성 프롬프트로 고친 사진을 다시 검수한 장면 (§13.1 45~60초). GET /api/checks/13 (목업)
 */
export const mockPassedCheckResult: CheckResult = {
  checkId: 13,
  asOf: "2026-11-14",
  overall: "GREEN",
  menuName: "딸기 크레이프 케이크",
  aiImageUrl: "/home/ai-menu-fixed.jpg",
  realImageUrl: "/home/real-menu.jpg",
  findings: [
    {
      ruleCode: "IMG_COUNT",
      severity: "GREEN",
      aiValue: "딸기 3개",
      realValue: "딸기 3개",
      evidence: "딸기 3개, 실물과 같아요",
    },
    { ruleCode: "IMG_EXTRA_ITEM", severity: "GREEN", evidence: "찾지 못했어요" },
    { ruleCode: "CAP_MEDICINE", severity: "GREEN", evidence: "찾지 못했어요" },
    {
      ruleCode: "INFO_AI_LABEL",
      severity: "GREEN",
      evidence: "“연출된 이미지입니다”가 들어 있어요",
    },
    {
      // 피그마 47:168 — 🟢 통과지만 한 번 확인
      ruleCode: "IMG_FILL",
      severity: "GREEN",
      checkOnce: true,
      aiValue: "크림 1.2배",
      evidence: "AI 사진 크림 양이 실물의 1.2배예요. 기준(1.5배) 아래라 통과예요.",
    },
  ],
  captionFix: "딸기 3개를 올린 크레이프 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: { ko: "", en: "" },
};

export const mockMenus: Menu[] = [
  {
    id: 1,
    name: "딸기 크레이프 케이크",
    facts: { name: "딸기 크레이프 케이크" },
    createdAt: "2026-11-14T18:00:00+09:00",
  },
];

/** 국가법령정보센터 조문 링크 */
const LAW = {
  food: "https://www.law.go.kr/법령/식품등의표시ㆍ광고에관한법률/제8조",
  fair: "https://www.law.go.kr/법령/표시ㆍ광고의공정화에관한법률/제3조",
  origin: "https://www.law.go.kr/법령/농수산물의원산지표시등에관한법률/제6조",
} as const;

const FOOD_FAIR_FALSE = "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호";

/**
 * GET /api/rules 목업 — OLLYEODO_MVP.md §6 판정 기준표 13행 그대로 (임의 변경 금지).
 * effectiveFrom 은 §5 현행 시행본 날짜를 넣은 목업 값이다.
 */
export const mockRules: Rule[] = [
  { code: "IMG_EXTRA_ITEM", title: "없는 재료", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "IMG_COUNT", title: "개수", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "IMG_FILL", title: "양, 크기", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "IMG_LAYER", title: "층 수, 구성", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_FACT", title: "수치·재료 불일치", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_ORIGIN", title: "원산지 불일치", lawRef: "원산지표시법 제6조①②", sourceUrl: LAW.origin, effectiveFrom: "2026-10-22", severity: "RED" },
  { code: "CAP_DISEASE", title: "질병 예방·치료", lawRef: "식품표시광고법 제8조①1호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_MEDICINE", title: "의약품 오인", lawRef: "식품표시광고법 제8조①2호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_HFOOD", title: "건강기능식품 오인", lawRef: "식품표시광고법 제8조①3호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_SUPERLATIVE", title: "근거 없는 최상급·비교", lawRef: "식품표시광고법 제8조①7호 · 표시광고법 제3조①3호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_FAKE_REVIEW", title: "꾸민 후기", lawRef: "식품표시광고법 제8조①5호 · 표시광고법 제3조①2호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  {
    code: "AI_VIRTUAL_EXPERT",
    title: "AI 가상 전문가",
    lawRef: "식품표시광고법 제8조①11호",
    sourceUrl: LAW.food,
    effectiveFrom: "2026-11-27",
    effectiveTo: null,
    severity: "RED",
    // §6.2: 11/26까지 🟢 + "11/27부터 금지" 안내
    preEffectiveSeverity: "GREEN",
  },
  { code: "INFO_AI_LABEL", title: "AI 생성 안내", lawRef: "판정 아님 (§5.2)", effectiveFrom: "2026-01-22", severity: "GREEN" },
];

/** 목업 결과 찾기 — 13번은 🟢, 나머지는 🔴 */
export const mockCheckById = (id: number): CheckResult =>
  id === mockPassedCheckResult.checkId
    ? mockPassedCheckResult
    : { ...mockCheckResult, checkId: id };
