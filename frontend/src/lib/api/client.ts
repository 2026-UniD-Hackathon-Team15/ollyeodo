import { env } from "@/lib/env";
import { mockApi } from "@/mocks/handlers";
import type {
  CheckResult,
  CreateCheckInput,
  CreateMenuInput,
  Menu,
  Rule,
} from "@/lib/types";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${env.apiBaseUrl}${path}`, init);
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new ApiError(res.status, text || res.statusText);
  }
  return res.json() as Promise<T>;
}

const realApi = {
  listMenus: () => request<Menu[]>("/api/menus"),

  createMenu: ({ facts, realImage }: CreateMenuInput) => {
    const form = new FormData();
    form.append("realImage", realImage);
    form.append(
      "facts",
      new Blob([JSON.stringify(facts)], { type: "application/json" }),
    );
    return request<Menu>("/api/menus", { method: "POST", body: form });
  },

  createCheck: async ({ menuName, realImage, aiImage, caption, asOf }: CreateCheckInput): Promise<CheckResult> => {
    const form = new FormData();
    form.append("realImage", realImage);
    form.append("aiImage", aiImage);
    form.append("caption", caption);
    if (asOf) form.append("asOf", asOf);
    const result = await request<CheckResult>("/api/checks", { method: "POST", body: form });
    // 메뉴 이름은 API 계약에 없어 화면에서 입력한 값을 붙여 둔다
    return { ...result, menuName: result.menuName ?? menuName };
  },

  getCheck: (id: number) => request<CheckResult>(`/api/checks/${id}`),

  listRules: () => request<Rule[]>("/api/rules"),
};

/** NEXT_PUBLIC_USE_MOCK 으로 실제 API ↔ 목업 전환 */
export const api: typeof realApi = env.useMock ? mockApi : realApi;
