package com.team.ollyeodo.api;

import com.team.ollyeodo.domain.Severity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/checks/stats 응답 (#20, MVP §3.1 기능 7 검수 기록).
 * ruleCounts: 🔴로 걸린 규칙별 검수 건수 (많은 순). 한 검수에서 같은 규칙이 여러 번 걸려도 1건으로 센다.
 * recentChecks: 최근 검수 목록 (최신순).
 */
public record CheckStatsResponse(long totalChecks, long redChecks, long greenChecks,
                                 List<RuleCount> ruleCounts, List<RecentCheck> recentChecks) {

    /** @param title 규칙 이름 (rules 테이블), lawRef 근거 조항 */
    public record RuleCount(String ruleCode, String title, String lawRef, long count) {}

    /** @param redCount 이 검수에서 🔴로 걸린 항목 수 */
    public record RecentCheck(Long checkId, String menuName, Severity overall, long redCount,
                              LocalDate asOf, Instant createdAt) {}
}
