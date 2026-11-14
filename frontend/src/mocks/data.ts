import type { CheckResult, CheckStats, Rule } from "@/lib/types";

/** 국가법령정보센터 조문 링크 */
const LAW = {
  food: "https://www.law.go.kr/법령/식품등의표시ㆍ광고에관한법률/제8조",
  fair: "https://www.law.go.kr/법령/표시ㆍ광고의공정화에관한법률/제3조",
  origin: "https://www.law.go.kr/법령/농수산물의원산지표시등에관한법률/제6조",
} as const;

const FOOD_FAIR_FALSE = "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호";

/**
 * 🔴 결과 목업 — 백엔드 응답 모양 그대로 (RuleEngine·ComparisonService·CaptionService 문구) +
 * 피그마 「03 검수 결과 — 고쳐 주세요」(13:169) 내용. GET /api/checks/12 (목업)
 * 백엔드는 걸린 항목과 사진 비교 값만 보낸다. 시행 전 규칙은 🟢 + checkOnce + 「— ○월 ○일부터 금지」.
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
      evidence: "실물의 3배",
      lawRef: FOOD_FAIR_FALSE,
    },
    {
      ruleCode: "IMG_EXTRA_ITEM",
      severity: "RED",
      aiValue: "블루베리",
      realValue: "없음",
      evidence: "실물에 없는 재료(블루베리)",
      lawRef: FOOD_FAIR_FALSE,
    },
    {
      ruleCode: "CAP_HFOOD",
      severity: "RED",
      aiValue: "혈당 걱정 없는",
      evidence: "'혈당 걱정 없는' — 건강기능식품 오인 표현",
      lawRef: "식품표시광고법 제8조①3호",
    },
    {
      ruleCode: "CAP_MEDICINE",
      severity: "RED",
      aiValue: "디톡스",
      evidence: "'디톡스' — 의약품 오인 표현",
      lawRef: "식품표시광고법 제8조①2호",
    },
    {
      // 1.2~1.5배는 🟢 (§6.1). 고칠 곳 화면에는 늘어놓지 않는다
      ruleCode: "IMG_FILL",
      severity: "GREEN",
      aiValue: "접시 대비 0.65",
      realValue: "접시 대비 0.5",
      evidence: "실물의 1.3배",
      lawRef: FOOD_FAIR_FALSE,
      checkOnce: false,
    },
    {
      // 시행 전(11/27) → 🟢 + 한 번 확인 (피그마 47:158 「통과지만 한 번 확인해 보세요」)
      ruleCode: "AI_VIRTUAL_EXPERT",
      severity: "GREEN",
      aiValue: "메뉴 설명의 전문가 추천 표현",
      evidence: "AI로 만든 전문가가 추천하는 것처럼 보일 수 있음 — 11월 27일부터 금지",
      lawRef: "식품표시광고법 제8조①11호",
      checkOnce: true,
    },
    {
      ruleCode: "INFO_AI_LABEL",
      severity: "GREEN",
      evidence: "'연출된 이미지입니다' 문구를 함께 적는 것을 권장해요",
      checkOnce: true,
    },
  ],
  captionFix: "딸기 3개를 올린 크레이프 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: {
    ko: "딸기 크레이프 케이크 1조각, 흰 원형 접시, 위에서 45도, 자연광.\n크레이프 사이 생크림과 딸기 조각, 윗면 딸기 3개. 실제 매장 사진과 같은 크기와 양.\n넣지 말 것: 블루베리, 딸기 4개 이상, 접시를 넘치는 크림, 글자.",
    en: "One slice of strawberry crepe cake on a white round plate, 45-degree top view, natural light.\nWhipped cream and strawberry pieces inside, exactly 3 strawberries on top. Same size and amount as the real store photo.\nAvoid: blueberries, 4 or more strawberries, cream overflowing the plate, text.",
  },
};

/**
 * 🟢 결과 목업 — 피그마 「03-2 검수 결과 — 올려도 됩니다」(13:485).
 * 재생성 프롬프트로 고친 사진을 다시 검수한 장면 (§13.1 45~60초). GET /api/checks/13 (목업)
 * 백엔드처럼 사진 비교 값만 온다 — 나머지 규칙의 「찾지 못했어요」는 화면이 규칙 목록으로 채운다.
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
      evidence: "실물의 1배",
      lawRef: FOOD_FAIR_FALSE,
      checkOnce: false,
    },
    {
      ruleCode: "IMG_FILL",
      severity: "GREEN",
      aiValue: "접시 대비 0.6",
      realValue: "접시 대비 0.5",
      evidence: "실물의 1.2배",
      lawRef: FOOD_FAIR_FALSE,
      checkOnce: false,
    },
  ],
  captionFix: "딸기 3개를 올린 크레이프 케이크예요. (사진은 연출된 이미지입니다)",
  regenPrompt: null, // 백엔드도 🟢면 null
};

/**
 * GET /api/rules 목업 — 백엔드 RuleSeedData(§6 판정 기준표 13행)와 같은 값 (임의 변경 금지).
 * sourceUrl 만 화면 확인용으로 조문 링크를 넣었다.
 */
export const mockRules: Rule[] = [
  { code: "IMG_EXTRA_ITEM", title: "없는 재료", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "IMG_COUNT", title: "개수", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "IMG_FILL", title: "양, 크기", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_FACT", title: "수치·재료 불일치 (실물 사진 추출값 기준)", lawRef: FOOD_FAIR_FALSE, sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  // 판정 아님 — 원산지 주장이 있으면 🟢 + 직접 확인 안내
  { code: "CAP_ORIGIN", title: "원산지 표기가 실제와 같은지 직접 확인해 주세요", lawRef: "원산지표시법 제5조, 제6조①②", sourceUrl: LAW.origin, effectiveFrom: "2026-10-22", severity: "GREEN" },
  { code: "CAP_DISEASE", title: "질병 예방·치료", lawRef: "식품표시광고법 제8조①1호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_MEDICINE", title: "의약품 오인", lawRef: "식품표시광고법 제8조①2호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_HFOOD", title: "건강기능식품 오인", lawRef: "식품표시광고법 제8조①3호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_SUPERLATIVE", title: "근거 없는 최상급·비교", lawRef: "식품표시광고법 제8조①7호 · 표시광고법 제3조①3호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  { code: "CAP_FAKE_REVIEW", title: "꾸민 후기", lawRef: "식품표시광고법 제8조①5호 · 표시광고법 제3조①2호", sourceUrl: LAW.food, effectiveFrom: "2025-09-19", severity: "RED" },
  {
    code: "AI_VIRTUAL_EXPERT",
    title: "AI 가상 전문가 — 11/27부터 금지",
    lawRef: "식품표시광고법 제8조①11호",
    sourceUrl: LAW.food,
    effectiveFrom: "2026-11-27",
    effectiveTo: null,
    severity: "RED",
    // §6.2: 11/26까지 🟢 + "11/27부터 금지" 안내
    preEffectiveSeverity: "GREEN",
  },
  // 판정 아님 — 백엔드는 근거 조항·시행일을 null 로 보낸다 (화면은 「—」로 표시)
  { code: "INFO_AI_LABEL", title: "연출 이미지 문구 권장", lawRef: "", effectiveFrom: "", severity: "GREEN" },
];

/** 피그마 「04 검수 기록」(68:153) 수치 — 전체 12 · 올려도 됨 7 · 고쳐야 함 5 */
export const mockStats: CheckStats = {
  totalChecks: 12,
  greenChecks: 7,
  redChecks: 5,
  ruleCounts: [
    { ruleCode: "IMG_COUNT", title: "개수", lawRef: FOOD_FAIR_FALSE, count: 7 },
    { ruleCode: "IMG_EXTRA_ITEM", title: "없는 재료", lawRef: FOOD_FAIR_FALSE, count: 5 },
    { ruleCode: "CAP_FACT", title: "수치·재료 불일치 (실물 사진 추출값 기준)", lawRef: FOOD_FAIR_FALSE, count: 4 },
    { ruleCode: "CAP_MEDICINE", title: "의약품 오인", lawRef: "식품표시광고법 제8조①2호", count: 3 },
    { ruleCode: "CAP_SUPERLATIVE", title: "근거 없는 최상급·비교", lawRef: "식품표시광고법 제8조①7호 · 표시광고법 제3조①3호", count: 2 },
    { ruleCode: "AI_VIRTUAL_EXPERT", title: "AI 가상 전문가 — 11/27부터 금지", lawRef: "식품표시광고법 제8조①11호", count: 1 },
  ],
  recentChecks: [
    { checkId: 14, menuName: "딸기 크레이프 케이크", overall: "GREEN", redCount: 0, asOf: "2026-11-14", createdAt: "2026-11-14T12:12:00Z" },
    { checkId: 12, menuName: "딸기 크레이프 케이크", overall: "RED", redCount: 4, asOf: "2026-11-14", createdAt: "2026-11-14T11:40:00Z" },
    { checkId: 13, menuName: "말차 티라미수", overall: "GREEN", redCount: 0, asOf: "2026-11-13", createdAt: "2026-11-13T03:00:00Z" },
    { checkId: 11, menuName: "바질 크림 파스타", overall: "RED", redCount: 2, asOf: "2026-11-12", createdAt: "2026-11-12T03:00:00Z" },
  ],
};

/** 목업 결과 찾기 — 🟢 목록(13·14)은 통과 결과, 나머지는 🔴 */
export const mockCheckById = (id: number): CheckResult =>
  id === 13 || id === 14
    ? { ...mockPassedCheckResult, checkId: id }
    : { ...mockCheckResult, checkId: id };
