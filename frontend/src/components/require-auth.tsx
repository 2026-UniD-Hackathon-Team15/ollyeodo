"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { env } from "@/lib/env";
import { useSession } from "@/lib/use-session";

/** 로그인해야 보이는 화면. 목업 모드에서는 로그인 없이 보여 준다. */
export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { session, ready } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const allowed = env.useMock || !!session;

  useEffect(() => {
    if (ready && !allowed) {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [ready, allowed, pathname, router]);

  if (!allowed) return <div className="min-h-[60vh]" />;
  return children;
}
