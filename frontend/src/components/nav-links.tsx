"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const nav = [
  { href: "/", label: "홈" },
  { href: "/checks/new", label: "검수하기" },
  { href: "/stats", label: "검수 기록" },
];

/** 검수 결과(/checks/12)는 「검수하기」 탭에 속한다 */
function isActive(href: string, pathname?: string) {
  if (!pathname) return false;
  if (href === "/") return pathname === "/";
  if (href === "/checks/new") return pathname.startsWith("/checks");
  return pathname.startsWith(href);
}

function Links({ pathname }: { pathname?: string }) {
  return (
    <nav className="flex items-center gap-5 text-[15px] leading-[23px] font-medium sm:gap-8">
      {nav.map((item) => {
        const active = isActive(item.href, pathname);
        return (
          <Link
            key={item.href}
            href={item.href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "transition-colors hover:text-ink",
              active ? "text-ink" : "text-sub",
            )}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}

export function NavLinks() {
  return <Links pathname={usePathname()} />;
}

/** Suspense fallback 용 — 현재 경로 강조 없이 링크만 */
export function NavLinksStatic() {
  return <Links />;
}
