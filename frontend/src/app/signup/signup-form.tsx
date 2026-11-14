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

const PASSWORD_MIN_LENGTH = 6;

function signupMessage(message: string) {
  if (/already registered|already been registered/i.test(message)) return "이미 가입된 이메일이에요.";
  if (/password/i.test(message)) return "비밀번호를 다시 확인해 주세요.";
  return "가입하지 못했어요. 잠시 뒤 다시 시도해 주세요.";
}

export function SignupForm() {
  const router = useRouter();
  const next = safeNext(useSearchParams().get("next"));
  const [storeName, setStoreName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [agreeTerms, setAgreeTerms] = useState(false);
  const [agreePrivacy, setAgreePrivacy] = useState(false);
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);

  const allAgreed = agreeTerms && agreePrivacy;

  function toggleAll(checked: boolean) {
    setAgreeTerms(checked);
    setAgreePrivacy(checked);
  }

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (password.length < PASSWORD_MIN_LENGTH) {
      setError(`비밀번호는 ${PASSWORD_MIN_LENGTH}자 이상이어야 해요.`);
      return;
    }
    if (password !== passwordConfirm) {
      setError("비밀번호가 서로 달라요.");
      return;
    }
    if (!supabase) {
      setError("회원가입 설정이 아직 없어요. 관리자에게 문의해 주세요.");
      return;
    }
    setPending(true);
    setError("");
    const { data, error: authError } = await supabase.auth.signUp({
      email,
      password,
      options: { data: { store_name: storeName } },
    });
    if (authError) {
      setError(signupMessage(authError.message));
      setPending(false);
      return;
    }
    if (!data.session) {
      setError("가입은 됐지만 바로 로그인되지 않았어요. 관리자에게 문의해 주세요.");
      setPending(false);
      return;
    }
    toast.success("가입했어요. 환영해요!");
    router.replace(next);
  }

  return (
    <AuthCard>
      <h1 className="text-[32px] leading-[44px] font-bold text-ink">회원가입</h1>
      <p className="mt-2.5 text-[17px] leading-[27px] text-sub">
        가게 이름과 이메일만 있으면 바로 시작할 수 있어요.
      </p>

      <form onSubmit={onSubmit} className="mt-9 flex flex-col gap-6">
        <AuthField
          label="가게 이름"
          autoComplete="organization"
          placeholder="예: 달콤 크레이프"
          value={storeName}
          onChange={(e) => setStoreName(e.target.value)}
        />
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
          autoComplete="new-password"
          required
          placeholder="비밀번호 입력"
          hint="6자 이상"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <AuthField
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          required
          placeholder="비밀번호 다시 입력"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
        />

        <div className="flex flex-col gap-3.5 rounded-2xl bg-surface-subtle p-[18px]">
          <label className="flex items-center gap-2.5 text-[15px] leading-6 font-bold text-ink">
            <input
              type="checkbox"
              checked={allAgreed}
              onChange={(e) => toggleAll(e.target.checked)}
              className="size-5 accent-signal-green-strong"
            />
            전체 동의
          </label>
          <div className="h-px bg-line" />
          <label className="flex items-center gap-2.5 text-[15px] leading-6 text-sub">
            <input
              type="checkbox"
              checked={agreeTerms}
              onChange={(e) => setAgreeTerms(e.target.checked)}
              className="size-5 accent-signal-green-strong"
            />
            [필수] 서비스 이용약관 동의
          </label>
          <label className="flex items-center gap-2.5 text-[15px] leading-6 text-sub">
            <input
              type="checkbox"
              checked={agreePrivacy}
              onChange={(e) => setAgreePrivacy(e.target.checked)}
              className="size-5 accent-signal-green-strong"
            />
            [필수] 개인정보 수집·이용 동의
          </label>
        </div>

        {error && (
          <p role="alert" className="text-[15px] leading-6 text-signal-red-ink">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={pending || !allAgreed}
          className="mt-2 h-14 rounded-2xl bg-primary text-[17px] font-bold text-white transition-colors hover:bg-signal-green-strong disabled:opacity-60"
        >
          {pending ? "가입하는 중…" : "가입하고 시작하기"}
        </button>
      </form>

      <p className="mt-6 text-center text-[15px] leading-6 text-faint">
        이미 계정이 있나요?{" "}
        <Link
          href={`/login${next === "/" ? "" : `?next=${encodeURIComponent(next)}`}`}
          className="font-medium text-signal-green-ink"
        >
          로그인
        </Link>
      </p>
    </AuthCard>
  );
}
