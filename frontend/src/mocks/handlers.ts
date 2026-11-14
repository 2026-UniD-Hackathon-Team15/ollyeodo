import type {
  CheckResult,
  CreateCheckInput,
  CreateMenuInput,
  Menu,
  Rule,
} from "@/lib/types";
import { mockCheckById, mockCheckResult, mockMenus, mockPassedCheckResult, mockRules } from "./data";

/** 실제 네트워크처럼 보이도록 약간 지연 */
const delay = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** api client 와 같은 모양의 목업 구현. NEXT_PUBLIC_USE_MOCK=true 일 때 사용된다. */
export const mockApi = {
  async listMenus(): Promise<Menu[]> {
    await delay(300);
    return mockMenus;
  },
  async createMenu(input: CreateMenuInput): Promise<Menu> {
    await delay(500);
    return { id: mockMenus.length + 1, name: input.facts.name, facts: input.facts };
  },
  /**
   * 데모 흐름(§13.1)을 목업으로도 보이게: 메뉴 설명에 🔴 예시 표현("혈당 걱정 없는", "디톡스")이 있으면 🔴,
   * 고친 문구로 다시 검수하면 🟢 결과를 돌려준다.
   */
  async createCheck(input: CreateCheckInput): Promise<CheckResult> {
    await delay(1500);
    const red = /혈당 걱정 없는|디톡스/.test(input.caption);
    const base = red ? mockCheckResult : mockPassedCheckResult;
    return { ...base, menuName: input.menuName, asOf: input.asOf ?? base.asOf };
  },
  async getCheck(id: number): Promise<CheckResult> {
    await delay(300);
    return mockCheckById(id);
  },
  async listRules(): Promise<Rule[]> {
    await delay(200);
    return mockRules;
  },
};
