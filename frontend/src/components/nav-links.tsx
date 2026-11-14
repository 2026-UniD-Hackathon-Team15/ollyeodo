"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const nav = [
  { href: "/menus/new", label: "메뉴 등록" },
  { href: "/checks/new", label: "검수하기" },
];

function Links({ pathname }: { pathname?: string }) {
  return (
    <nav className="flex gap-1 text-sm">
      {nav.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className={cn(
            "rounded-md px-3 py-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground",
            pathname?.startsWith(item.href) && "bg-muted font-medium text-foreground",
          )}
        >
          {item.label}
        </Link>
      ))}
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
