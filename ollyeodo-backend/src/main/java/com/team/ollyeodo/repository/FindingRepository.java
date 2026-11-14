package com.team.ollyeodo.repository;

import com.team.ollyeodo.domain.Finding;
import com.team.ollyeodo.domain.Severity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingRepository extends JpaRepository<Finding, Long> {

    /** 규칙별로 그 규칙이 걸린 검수 건수 (한 검수에서 같은 규칙이 여러 번 걸려도 1건) */
    @Query("""
            select f.ruleCode as ruleCode, count(distinct f.check.id) as checkCount
            from Finding f where f.severity = :severity
            group by f.ruleCode
            order by count(distinct f.check.id) desc, f.ruleCode asc""")
    List<RuleCount> countChecksByRule(@Param("severity") Severity severity);

    /** 검수별 특정 심각도 항목 수 */
    @Query("""
            select f.check.id as checkId, count(f) as findingCount
            from Finding f where f.severity = :severity and f.check.id in :checkIds
            group by f.check.id""")
    List<CheckFindingCount> countByCheck(@Param("severity") Severity severity,
                                         @Param("checkIds") Collection<Long> checkIds);

    interface RuleCount {
        String getRuleCode();
        long getCheckCount();
    }

    interface CheckFindingCount {
        Long getCheckId();
        long getFindingCount();
    }
}
