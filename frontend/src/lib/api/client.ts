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

  createCheck: ({ menuId, aiImage, caption, asOf }: CreateCheckInput) => {
    const form = new FormData();
    form.append("menuId", String(menuId));
    form.append("aiImage", aiImage);
    form.append("caption", caption);
    if (asOf) form.append("asOf", asOf);
    return request<CheckResult>("/api/checks", { method: "POST", body: form });
  },

  getCheck: (id: number) => request<CheckResult>(`/api/checks/${id}`),

  listRules: () => request<Rule[]>("/api/rules"),
};

/** NEXT_PUBLIC_USE_MOCK 으로 실제 API ↔ 목업 전환 */
export const api: typeof realApi = env.useMock ? mockApi : realApi;
