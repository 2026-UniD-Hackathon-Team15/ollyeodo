import Link from "next/link";
import { Suspense } from "react";
import { NavLinks, NavLinksStatic } from "./nav-links";

export function SiteHeader() {
  return (
    <header className="sticky top-0 z-10 border-b bg-background/80 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-5xl items-center justify-between px-4">
        <Link href="/" className="flex items-center gap-2 font-bold">
          <span aria-hidden>🚦</span>
          올려도 될까
        </Link>
        {/* 현재 경로(usePathname)는 요청 시점 값이라 Suspense 로 감싼다 (cacheComponents) */}
        <Suspense fallback={<NavLinksStatic />}>
          <NavLinks />
        </Suspense>
      </div>
    </header>
  );
}
