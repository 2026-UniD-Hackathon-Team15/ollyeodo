import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { TrafficLight } from "@/components/traffic-light";

export default function Home() {
  return (
    <section className="flex flex-col items-center gap-6 py-16 text-center">
      <TrafficLight severity="GREEN" />
      <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">
        AI로 만든 메뉴 홍보물, 올려도 될까요?
      </h1>
      <p className="max-w-xl text-muted-foreground">
        실제 메뉴 정보와 비교해 다른 곳을 신호등으로 알려 드리고, 고친 문구와 다시 만들
        프롬프트까지 드려요.
      </p>
      <div className="flex gap-3">
        <Link href="/checks/new" className={buttonVariants()}>
          검수 시작하기
        </Link>
        <Link href="/menus/new" className={buttonVariants({ variant: "outline" })}>
          메뉴 먼저 등록
        </Link>
      </div>
    </section>
  );
}
