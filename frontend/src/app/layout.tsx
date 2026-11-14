import type { Metadata } from "next";
import { Inter, Noto_Sans_KR } from "next/font/google";
import { MockBanner } from "@/components/mock-banner";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { SiteFooter } from "@/components/site-footer";
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
  title: "Re:Ad",
  description:
    "직접 찍은 실제 메뉴 사진과 AI로 만든 메뉴 사진을 대조해, 무엇이 얼마나 다른지 근거 조항과 함께 찾아 주는 서비스",
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
          <main className="flex w-full flex-1 flex-col">{children}</main>
          <SiteFooter />
        </Providers>
      </body>
    </html>
  );
}
