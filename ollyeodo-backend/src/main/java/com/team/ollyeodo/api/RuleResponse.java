package com.team.ollyeodo.api;

import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;

public record RuleResponse(String code, String title, String lawRef, String sourceUrl,
                           LocalDate effectiveFrom, LocalDate effectiveTo,
                           Severity severity, Severity preEffectiveSeverity, boolean upcoming) {
    public static RuleResponse from(Rule rule, LocalDate asOf) {
        return new RuleResponse(rule.getCode(), rule.getTitle(), rule.getLawRef(), rule.getSourceUrl(),
                rule.getEffectiveFrom(), rule.getEffectiveTo(), rule.getSeverity(), rule.getPreEffectiveSeverity(),
                rule.getEffectiveFrom() != null && asOf.isBefore(rule.getEffectiveFrom()));
    }
}
