import type { Metadata } from "next";
import { Inter, Noto_Sans_KR } from "next/font/google";
import { MockBanner } from "@/components/mock-banner";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { DISCLAIMER } from "@/lib/severity";
import "./globals.css";

// 본문 글꼴 — 피그마와 같은 Noto Sans KR (한글 글자는 unicode-range 조각으로 나눠 받는다)
const notoSansKr = Noto_Sans_KR({
  variable: "--font-noto-sans-kr",
  weight: ["400", "500", "700"],
  preload: false,
  display: "swap",
});

// 로고(Re:Ad)·큰 숫자 전용
const inter = Inter({
  variable: "--font-inter",
  weight: ["800"],
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "올려도 될까 — AI 홍보물 검수",
  description:
    "AI로 만든 메뉴 사진과 문구가 실제 메뉴와 다른 곳을 신호등으로 알려주고 고쳐 주는 서비스",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="ko"
      className={`${notoSansKr.variable} ${inter.variable} h-full antialiased`}
    >
      <body className="flex min-h-full flex-col">
        <Providers>
          <MockBanner />
          <SiteHeader />
          <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">{children}</main>
          <footer className="border-t py-6 text-center text-xs text-muted-foreground">
            {DISCLAIMER}
          </footer>
        </Providers>
      </body>
    </html>
  );
}
