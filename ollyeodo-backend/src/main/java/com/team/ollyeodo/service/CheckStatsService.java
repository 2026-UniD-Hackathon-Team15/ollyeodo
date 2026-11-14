package com.team.ollyeodo.service;

import com.team.ollyeodo.api.CheckStatsResponse;
import com.team.ollyeodo.api.CheckStatsResponse.RecentCheck;
import com.team.ollyeodo.api.CheckStatsResponse.RuleCount;
import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.CheckRepository;
import com.team.ollyeodo.repository.FindingRepository;
import com.team.ollyeodo.repository.RuleRepository;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 검수 기록 통계 (#20). 로그인한 사용자 기록만 센다 (#71). 판정은 다시 하지 않는다. */
@Service
public class CheckStatsService {
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 50;

    private final CheckRepository checks;
    private final FindingRepository findings;
    private final RuleRepository rules;

    public CheckStatsService(CheckRepository checks, FindingRepository findings, RuleRepository rules) {
        this.checks = checks;
        this.findings = findings;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public CheckStatsResponse stats(UUID userId, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(MAX_LIMIT, limit));
        Map<String, Rule> byCode = rules.findAll().stream()
                .filter(r -> r.getCode() != null)
                .collect(Collectors.toMap(Rule::getCode, Function.identity(), (a, b) -> a));

        List<RuleCount> ruleCounts = findings.countChecksByRule(userId, Severity.RED).stream()
                .map(c -> {
                    Rule rule = byCode.get(c.getRuleCode());
                    return new RuleCount(c.getRuleCode(), rule == null ? null : rule.getTitle(),
                            rule == null ? null : rule.getLawRef(), c.getCheckCount());
                })
                .toList();

        List<Check> recent = checks.findRecent(userId, PageRequest.of(0, size));
        Map<Long, Long> redByCheck = recent.isEmpty() ? Map.of()
                : findings.countByCheck(Severity.RED, recent.stream().map(Check::getId).toList()).stream()
                .collect(Collectors.toMap(FindingRepository.CheckFindingCount::getCheckId,
                        FindingRepository.CheckFindingCount::getFindingCount));
        List<RecentCheck> recentChecks = recent.stream()
                .map(c -> new RecentCheck(c.getId(), c.getMenuName(), c.getOverall(),
                        redByCheck.getOrDefault(c.getId(), 0L), c.getAsOf(), c.getCreatedAt()))
                .toList();

        return new CheckStatsResponse(checks.countByUserId(userId),
                checks.countByUserIdAndOverall(userId, Severity.RED),
                checks.countByUserIdAndOverall(userId, Severity.GREEN), ruleCounts, recentChecks);
    }
}
