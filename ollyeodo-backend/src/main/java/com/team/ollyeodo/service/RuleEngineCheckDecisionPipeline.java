package com.team.ollyeodo.service;

import com.team.ollyeodo.api.CheckResponse.FindingResponse;
import com.team.ollyeodo.repository.RuleRepository;
import org.springframework.stereotype.Component;

/**
 * #11 통합 경계의 실제 구현: RuleEngine(#9) 판정 → RegenPromptBuilder(#10) 개선 프롬프트.
 * 판정은 코드(RuleEngine)가 하고, 규칙(근거 조항·시행일·키워드)은 rules 테이블(#5)에서 읽는다.
 */
@Component
public class RuleEngineCheckDecisionPipeline implements CheckDecisionPipeline {
    private final RuleEngine ruleEngine;
    private final RegenPromptBuilder regenPromptBuilder;
    private final RuleRepository rules;

    public RuleEngineCheckDecisionPipeline(RuleEngine ruleEngine, RegenPromptBuilder regenPromptBuilder,
                                           RuleRepository rules) {
        this.ruleEngine = ruleEngine;
        this.regenPromptBuilder = regenPromptBuilder;
        this.rules = rules;
    }

    @Override
    public Result evaluate(Input input) {
        JudgeResult judged = ruleEngine.judge(new JudgeInput(input.asOf(), input.run1(), input.run2(),
                input.captionClaims(), input.realFirst(), input.aiFirst(), input.aiSecond()), rules.findAll());
        RegenOutput regen = regenPromptBuilder.build(judged, input.realFirst(), input.caption());
        var findings = judged.findings().stream()
                .map(f -> new FindingResponse(f.ruleCode(), f.severity(), f.aiValue(), f.realValue(),
                        f.evidence(), f.lawRef(), f.checkOnce()))
                .toList();
        return new Result(judged.overall(), findings, regen.captionFix(), regen.regenPrompt());
    }
}
