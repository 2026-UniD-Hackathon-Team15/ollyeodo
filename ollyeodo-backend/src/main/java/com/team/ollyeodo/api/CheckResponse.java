package com.team.ollyeodo.api;

import com.team.ollyeodo.domain.Check;
import com.team.ollyeodo.domain.Finding;
import com.team.ollyeodo.domain.RegenPrompt;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;
import java.util.List;

/** POST/GET 공통 응답. 엔티티와 양방향 관계를 직접 직렬화하지 않는다. */
public record CheckResponse(Long checkId, LocalDate asOf, Severity overall, String menuName, String caption,
                            String realImageUrl, String aiImageUrl,
                            List<FindingResponse> findings, String captionFix, RegenPrompt regenPrompt) {
    public record FindingResponse(String ruleCode, Severity severity, String aiValue, String realValue,
                                  String evidence, String lawRef, boolean checkOnce) {
        public static FindingResponse from(Finding finding) {
            return new FindingResponse(finding.getRuleCode(), finding.getSeverity(), finding.getAiValue(),
                    finding.getRealValue(), finding.getEvidence(), finding.getLawRef(), finding.isCheckOnce());
        }
    }

    public static CheckResponse from(Check check, String realUrl, String aiUrl) {
        return new CheckResponse(check.getId(), check.getAsOf(), check.getOverall(),
                check.getMenuName(), check.getCaption(), realUrl, aiUrl,
                check.getFindings().stream().map(FindingResponse::from).toList(),
                check.getCaptionFix(), check.getRegenPrompt());
    }
}
