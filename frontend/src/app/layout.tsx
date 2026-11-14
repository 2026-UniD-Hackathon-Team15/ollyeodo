import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { MockBanner } from "@/components/mock-banner";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { DISCLAIMER } from "@/lib/severity";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
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
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
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
