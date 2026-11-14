"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { toast } from "sonner";
import { SeverityBadge, TrafficLight } from "@/components/traffic-light";
import { Badge } from "@/components/ui/badge";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useCheck } from "@/lib/api/queries";
import { DISCLAIMER, ruleTitle, severityMeta } from "@/lib/severity";
import type { Finding } from "@/lib/types";
import { CheckResultSkeleton } from "./check-result-skeleton";

async function copy(text: string, what: string) {
  try {
    await navigator.clipboard.writeText(text);
    toast.success(`${what}을(를) 복사했어요`);
  } catch {
    toast.error("복사하지 못했어요. 직접 선택해서 복사해 주세요.");
  }
}

function FindingCard({ finding }: { finding: Finding }) {
  return (
    <Card size="sm">
      <CardHeader>
        <CardTitle className="text-base">{ruleTitle[finding.ruleCode]}</CardTitle>
        <CardAction>
          <SeverityBadge severity={finding.severity} />
        </CardAction>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        {(finding.aiValue || finding.realValue) && (
          <dl className="grid grid-cols-2 gap-2">
            <div className="rounded-lg bg-muted p-3">
              <dt className="text-xs text-muted-foreground">AI 메뉴 사진</dt>
              <dd className="font-semibold">{finding.aiValue ?? "—"}</dd>
            </div>
            <div className="rounded-lg bg-muted p-3">
              <dt className="text-xs text-muted-foreground">실제 메뉴</dt>
              <dd className="font-semibold">{finding.realValue ?? "—"}</dd>
            </div>
          </dl>
        )}
        {finding.evidence && <p>{finding.evidence}</p>}
        {finding.lawRef && (
          <p className="text-xs text-muted-foreground">
            이 조항에 걸릴 수 있는 차이예요 · {finding.lawRef}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

export function CheckResultView() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { data, isPending, isError, error, refetch } = useCheck(id);

  if (!Number.isFinite(id)) {
    return <p className="text-muted-foreground">잘못된 검수 번호예요.</p>;
  }
  if (isPending) return <CheckResultSkeleton />;
  if (isError) {
    return (
      <div className="space-y-3 text-center">
        <p>결과를 불러오지 못했어요. ({error.message})</p>
        <Button variant="outline" onClick={() => refetch()}>
          다시 시도
        </Button>
      </div>
    );
  }

  const meta = severityMeta[data.overall];

  return (
    <div className="space-y-8">
      {/* 종합 신호 */}
      <Card>
        <CardContent className="flex items-center gap-6">
          <TrafficLight state={data.overall} />
          <div className="space-y-1">
            <p className="text-sm text-muted-foreground">
              검수 #{data.checkId} · 기준일 {data.asOf}
            </p>
            <h1 className="text-2xl font-bold">
              {meta.emoji} {data.overall === "GREEN" ? "올려도 됩니다" : meta.label}
            </h1>
            <p className="text-muted-foreground">{meta.description}</p>
          </div>
        </CardContent>
      </Card>

      {/* 항목별 결과 */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">
          항목별 결과 <Badge variant="secondary">{data.findings.length}</Badge>
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {data.findings.map((f, i) => (
            <FindingCard key={`${f.ruleCode}-${i}`} finding={f} />
          ))}
        </div>
      </section>

      {/* 캡션 수정안 */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">고친 문구</h2>
        <Card>
          <CardContent className="flex items-start justify-between gap-4">
            <p className="leading-relaxed">{data.captionFix}</p>
            <Button size="sm" variant="outline" onClick={() => copy(data.captionFix, "문구")}>
              복사
            </Button>
          </CardContent>
        </Card>
      </section>

      {/* 재생성 프롬프트 */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold">다시 만들 때 쓸 프롬프트</h2>
        <Tabs defaultValue="ko">
          <TabsList>
            <TabsTrigger value="ko">한국어</TabsTrigger>
            <TabsTrigger value="en">English</TabsTrigger>
          </TabsList>
          {(["ko", "en"] as const).map((lang) => (
            <TabsContent key={lang} value={lang}>
              <Card>
                <CardContent className="flex items-start justify-between gap-4">
                  <p className="font-mono text-sm leading-relaxed">{data.regenPrompt[lang]}</p>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => copy(data.regenPrompt[lang], "프롬프트")}
                  >
                    복사
                  </Button>
                </CardContent>
              </Card>
            </TabsContent>
          ))}
        </Tabs>
      </section>

      <div className="flex flex-col items-center gap-3">
        <Link href="/checks/new" className={buttonVariants({ size: "lg" })}>
          다시 검수
        </Link>
        <p className="text-xs text-muted-foreground">{DISCLAIMER}</p>
      </div>
    </div>
  );
}
