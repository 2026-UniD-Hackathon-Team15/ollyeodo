import Link from "next/link";
import { Suspense } from "react";
import { Logo } from "./logo";
import { NavLinks, NavLinksStatic } from "./nav-links";

/** 피그마 Header v2 (11:90) — 높이 64, 좌우 40 */
export function SiteHeader() {
  return (
    <header className="sticky top-0 z-20 bg-white/90 backdrop-blur">
      <div className="flex h-16 items-center justify-between px-4 sm:px-10">
        <Link href="/" aria-label="Re:Ad 홈">
          <Logo />
        </Link>
        {/* 현재 경로(usePathname)는 요청 시점 값이라 Suspense 로 감싼다 (cacheComponents) */}
        <Suspense fallback={<NavLinksStatic />}>
          <NavLinks />
        </Suspense>
      </div>
    </header>
  );
}
