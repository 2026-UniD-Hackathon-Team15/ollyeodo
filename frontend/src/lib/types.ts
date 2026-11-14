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
  /**
   * 결과 화면 상단에 쓰는 값 (피그마 「03 검수 결과」). §9.1 예시에는 아직 없어 선택 값으로 둔다.
   * 백엔드가 보내 주면 그대로 쓰고, 없으면 화면이 그 부분을 생략한다.
   */
  menuName?: string;
  aiImageUrl?: string;
  realImageUrl?: string;
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

/** POST /api/checks 입력 — multipart realImage · aiImage · caption · asOf (§9, docs/check-api.md) */
export interface CreateCheckInput {
  /** 백엔드로는 보내지 않고 결과 화면 제목에만 쓴다 (API 계약에 없음) */
  menuName: string;
  realImage: File;
  aiImage: File;
  caption: string;
  /** 생략하면 서버가 오늘(Asia/Seoul)로 처리 */
  asOf?: string;
}

export interface CreateMenuInput {
  facts: MenuFacts;
  realImage: File;
}
