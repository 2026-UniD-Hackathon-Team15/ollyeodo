"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { AuthCard, AuthField } from "@/components/auth/auth-field";
import { supabase } from "@/lib/supabase";

function safeNext(value: string | null) {
  return value && value.startsWith("/") && !value.startsWith("//") ? value : "/";
}

function loginMessage(message: string) {
  if (message.includes("Invalid login credentials")) return "이메일 또는 비밀번호가 맞지 않아요.";
  return "로그인하지 못했어요. 잠시 뒤 다시 시도해 주세요.";
}

export function LoginForm() {
  const router = useRouter();
  const next = safeNext(useSearchParams().get("next"));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!supabase) {
      setError("로그인 설정이 아직 없어요. 관리자에게 문의해 주세요.");
      return;
    }
    setPending(true);
    setError("");
    const { error: authError } = await supabase.auth.signInWithPassword({ email, password });
    if (authError) {
      setError(loginMessage(authError.message));
      setPending(false);
      return;
    }
    toast.success("로그인했어요.");
    router.replace(next);
  }

  return (
    <AuthCard>
      <h1 className="text-[32px] leading-[44px] font-bold text-ink">로그인</h1>
      <p className="mt-2.5 text-[17px] leading-[27px] text-sub">
        가게 계정으로 들어가면 검수 기록을 이어서 볼 수 있어요.
      </p>

      <form onSubmit={onSubmit} className="mt-9 flex flex-col gap-6">
        <AuthField
          label="이메일"
          type="email"
          autoComplete="email"
          required
          placeholder="ceo@store.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <AuthField
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          required
          placeholder="비밀번호 입력"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        {error && (
          <p role="alert" className="text-[15px] leading-6 text-signal-red-ink">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={pending}
          className="mt-2 h-14 rounded-2xl bg-primary text-[17px] font-bold text-white transition-colors hover:bg-signal-green-strong disabled:opacity-60"
        >
          {pending ? "로그인하는 중…" : "로그인"}
        </button>
      </form>

      <p className="mt-6 text-center text-[15px] leading-6 text-faint">
        아직 계정이 없나요?{" "}
        <Link
          href={`/signup${next === "/" ? "" : `?next=${encodeURIComponent(next)}`}`}
          className="font-medium text-signal-green-ink"
        >
          회원가입
        </Link>
      </p>
    </AuthCard>
  );
}
