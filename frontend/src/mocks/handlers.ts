import type {
  CheckResult,
  CreateCheckInput,
  CreateMenuInput,
  Menu,
  Rule,
} from "@/lib/types";
import { mockCheckResult, mockMenus, mockRules } from "./data";

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
  async createCheck(input: CreateCheckInput): Promise<CheckResult> {
    await delay(1500);
    return { ...mockCheckResult, asOf: input.asOf ?? mockCheckResult.asOf };
  },
  async getCheck(id: number): Promise<CheckResult> {
    await delay(300);
    return { ...mockCheckResult, checkId: id };
  },
  async listRules(): Promise<Rule[]> {
    await delay(200);
    return mockRules;
  },
};
