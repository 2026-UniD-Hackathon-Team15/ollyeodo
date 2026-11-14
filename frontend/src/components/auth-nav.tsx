"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { supabase } from "@/lib/supabase";
import { useSession } from "@/lib/use-session";

export function AuthNav() {
  const { session } = useSession();
  const router = useRouter();

  if (!session) {
    return (
      <Link href="/login" className="text-[15px] leading-[23px] font-medium text-sub transition-colors hover:text-ink">
        로그인
      </Link>
    );
  }

  async function onLogout() {
    await supabase?.auth.signOut();
    router.replace("/login");
  }

  return (
    <div className="flex items-center gap-3 text-[15px] leading-[23px] font-medium text-sub">
      <span className="max-w-[160px] truncate">{session.user.email}</span>
      <button type="button" onClick={onLogout} className="transition-colors hover:text-ink">
        로그아웃
      </button>
    </div>
  );
}
