package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.api.CheckResponse.FindingResponse;
import com.team.ollyeodo.domain.RegenPrompt;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;
import java.util.List;

/** #9 RuleEngine과 #10 RegenPromptBuilder의 통합 지점. 확정 클래스명/계약을 선점하지 않는다. */
public interface CheckDecisionPipeline {
    Result evaluate(Input input);

    record Input(LocalDate asOf, ComparisonResult run1, ComparisonResult run2,
                 CaptionClaims captionClaims, ExtractedAttrs realFirst,
                 ExtractedAttrs aiFirst, ExtractedAttrs aiSecond, String caption) {}

    record Result(Severity overall, List<FindingResponse> findings,
                  String captionFix, RegenPrompt regenPrompt) {}
}
