/**
 * 백엔드 API 타입 — OLLYEODO_MVP.md §8, §9 기준.
 * 필드 이름을 바꾸지 말 것 (백엔드 응답과 1:1).
 */

export type Severity = "GREEN" | "YELLOW" | "RED";

export type RuleCode =
  | "IMG_EXTRA_ITEM"
  | "IMG_COUNT"
  | "IMG_FILL"
  | "IMG_LAYER"
  | "CAP_FACT"
  | "CAP_ORIGIN"
  | "CAP_DISEASE"
  | "CAP_MEDICINE"
  | "CAP_HFOOD"
  | "CAP_SUPERLATIVE"
  | "CAP_FAKE_REVIEW"
  | "AI_VIRTUAL_EXPERT"
  | "INFO_AI_LABEL";

/** 항목별 판정 결과 (§9.1 findings[]) */
export interface Finding {
  ruleCode: RuleCode;
  severity: Severity;
  aiValue?: string;
  realValue?: string;
  evidence?: string;
  lawRef?: string;
}

/** POST /api/checks, GET /api/checks/{id} 응답 (§9.1) */
export interface CheckResult {
  checkId: number;
  /** YYYY-MM-DD (Asia/Seoul) */
  asOf: string;
  overall: Severity;
  findings: Finding[];
  captionFix: string;
  regenPrompt: { ko: string; en: string };
}

/**
 * 메뉴 정보 (§8 Menu.facts). 재료·개수·원산지는 사장님이 입력하지 않는다 —
 * 매장에서 찍은 실제 메뉴 사진이 비교 기준이다 (피그마 「01 검수하기」).
 */
export interface MenuFacts {
  name: string;
}

/** POST /api/menus, GET /api/menus 응답 */
export interface Menu {
  id: number;
  name: string;
  facts: MenuFacts;
  realImageUrl?: string;
  createdAt?: string;
}

/** GET /api/rules 응답 (§8 Rule) */
export interface Rule {
  code: RuleCode;
  title: string;
  lawRef: string;
  sourceUrl?: string;
  effectiveFrom: string;
  effectiveTo?: string | null;
  severity: Severity;
  preEffectiveSeverity?: Severity | null;
}

/** GET /api/checks/stats 응답 — 검수 기록 화면 (이슈 #21, 백엔드 #20 대기 중이라 경로는 임시) */
export interface CheckStats {
  totalChecks: number;
  redChecks: number;
  greenChecks: number;
  ruleCounts: { ruleCode: RuleCode; count: number }[];
  recentChecks: RecentCheck[];
}

export interface RecentCheck {
  checkId: number;
  menuName: string;
  /** ISO 8601 (Asia/Seoul 시각) */
  checkedAt: string;
  overall: Severity;
  note: string;
  aiImageUrl?: string;
}

/** POST /api/checks 입력 — multipart menuName · realImage · aiImage · caption · asOf (§9, docs/check-api.md) */
export interface CreateCheckInput {
  menuId: number;
  aiImage: File;
  caption: string;
  /** 생략하면 서버가 오늘(Asia/Seoul)로 처리 */
  asOf?: string;
}

export interface CreateMenuInput {
  facts: MenuFacts;
  realImage: File;
}
