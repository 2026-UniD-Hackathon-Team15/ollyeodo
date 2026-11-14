import { env } from "@/lib/env";
import { mockApi } from "@/mocks/handlers";
import type { CheckResult, CheckStats, CreateCheckInput, Rule } from "@/lib/types";

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * 백엔드 오류 응답에서 사람이 읽을 한국어 문장만 꺼낸다.
 * ProblemDetail 은 `detail`, 스프링 기본 오류 응답은 `message`(설정에 따라 비어 있음)에 담긴다.
 * 못 찾으면 빈 문자열 — 화면이 상태 코드에 맞는 문구를 대신 쓴다.
 */
function serverMessage(text: string) {
  try {
    const body = JSON.parse(text) as { detail?: unknown; message?: unknown };
    for (const value of [body.detail, body.message]) {
      if (typeof value === "string" && value.trim()) return value.trim();
    }
  } catch {
    // JSON 이 아님
  }
  return "";
}

/** 상태 코드별 기본 안내 (docs/check-api.md 400·422·404·503) */
function statusMessage(status: number) {
  switch (status) {
    case 400:
      return "사진과 메뉴 설명을 다시 확인해 주세요.";
    case 404:
      return "검수 결과를 찾을 수 없어요.";
    case 422:
      return "사진을 읽지 못했어요. 실제 메뉴 사진을 다시 찍어 주세요.";
    case 503:
      return "지금은 검수할 수 없어요. 잠시 뒤 다시 시도해 주세요.";
    default:
      return "서버에 문제가 생겼어요. 잠시 뒤 다시 시도해 주세요.";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${env.apiBaseUrl}${path}`, init);
  } catch {
    // 서버가 꺼져 있거나 네트워크가 끊김
    throw new ApiError(0, "서버에 연결하지 못했어요. 잠시 뒤 다시 시도해 주세요.");
  }
  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new ApiError(res.status, serverMessage(text) || statusMessage(res.status));
  }
  return res.json() as Promise<T>;
}

const realApi = {
  createCheck: ({ menuName, realImage, aiImage, caption, asOf }: CreateCheckInput) => {
    const form = new FormData();
    form.append("menuName", menuName);
    form.append("realImage", realImage);
    form.append("aiImage", aiImage);
    form.append("caption", caption);
    if (asOf) form.append("asOf", asOf);
    return request<CheckResult>("/api/checks", { method: "POST", body: form });
  },

  getCheck: (id: number) => request<CheckResult>(`/api/checks/${id}`),

  listRules: () => request<Rule[]>("/api/rules"),

  getStats: () => request<CheckStats>("/api/checks/stats"),
};

/** NEXT_PUBLIC_USE_MOCK 으로 실제 API ↔ 목업 전환 */
export const api: typeof realApi = env.useMock ? mockApi : realApi;
