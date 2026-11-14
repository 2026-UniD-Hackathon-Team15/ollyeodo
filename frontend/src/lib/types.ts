/**
 * 백엔드 API 타입 — OLLYEODO_MVP.md §8, §9 기준.
 * 필드 이름을 바꾸지 말 것 (백엔드 응답과 1:1).
 */

export type Severity = "GREEN" | "YELLOW" | "RED";

export type RuleCode =
  | "IMG_EXTRA_ITEM"
  | "IMG_COUNT"
  | "IMG_FILL"
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
  /**
   * 🟢 이지만 올리기 전에 사장님이 직접 한 번 봐야 할 항목 (피그마 「통과지만 한 번 확인해 보세요」).
   * 판정(severity)은 바꾸지 않는 표시용 값이다. 기준은 팀 합의 후 §6에 추가 (이슈 #4·#8·#9·#11).
   */
  checkOnce?: boolean;
}

/** POST /api/checks, GET /api/checks/{id} 응답 (§9.1) */
export interface CheckResult {
  checkId: number;
  /** YYYY-MM-DD (Asia/Seoul) */
  asOf: string;
  overall: Severity;
  findings: Finding[];
  captionFix: string;
  /** 🟢면 백엔드가 null 을 준다 (docs/check-api.md) */
  regenPrompt: { ko: string; en: string } | null;
  /** 검수한 메뉴 이름 (POST 요청의 menuName 을 백엔드가 저장해 응답에 돌려준다) */
  menuName?: string;
  aiImageUrl?: string;
  realImageUrl?: string;
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

/** GET /api/checks/stats 응답 (CheckStatsResponse) */
export interface CheckStats {
  totalChecks: number;
  redChecks: number;
  greenChecks: number;
  /** 🔴로 걸린 규칙별 건수, 많은 순 */
  ruleCounts: RuleCount[];
  recentChecks: RecentCheck[];
}

export interface RuleCount {
  ruleCode: RuleCode;
  title: string | null;
  lawRef: string | null;
  count: number;
}

export interface RecentCheck {
  checkId: number;
  menuName: string;
  overall: Severity;
  redCount: number;
  /** YYYY-MM-DD (Asia/Seoul) */
  asOf: string;
  /** ISO 8601 */
  createdAt: string;
}

/** POST /api/checks 입력 — multipart menuName · realImage · aiImage · caption · asOf (§9, docs/check-api.md) */
export interface CreateCheckInput {
  menuName: string;
  realImage: File;
  aiImage: File;
  caption: string;
  /** 생략하면 서버가 오늘(Asia/Seoul)로 처리 */
  asOf?: string;
}
