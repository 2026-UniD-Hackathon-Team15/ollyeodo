package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import com.team.ollyeodo.repository.RuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** #9 RuleEngine + #10 RegenPromptBuilder가 #11 통합 경계에서 실제로 맞물리는지 (데모 시나리오 축소판). */
class RuleEngineCheckDecisionPipelineTest {

    private static Rule rule(String code, String lawRef, Severity severity, LocalDate from, String... keywords) {
        Rule r = new Rule();
        r.setCode(code);
        r.setTitle(code);
        r.setLawRef(lawRef);
        r.setEffectiveFrom(from);
        r.setSeverity(severity);
        r.setPreEffectiveSeverity(Severity.GREEN);
        r.setKeywords(List.of(keywords));
        return r;
    }

    private static ExtractedAttrs attrs(int count, int layers, String... extra) {
        var items = new java.util.ArrayList<ExtractedAttrs.Item>();
        items.add(new ExtractedAttrs.Item("딸기", count, "윗면"));
        for (String e : extra) items.add(new ExtractedAttrs.Item(e, 2, "윗면"));
        return new ExtractedAttrs(items, layers, 0.6, "흰 접시", List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("데모: 딸기 2.25배·블루베리·12겹·디톡스 → RED + 사유·근거 조항 + 개선 프롬프트·문구 수정안")
    void demoScenarioEndToEnd() {
        LocalDate law = LocalDate.of(2025, 9, 19);
        String fact = "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호";
        RuleRepository repo = mock(RuleRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                rule("IMG_EXTRA_ITEM", fact, Severity.RED, law),
                rule("IMG_COUNT", fact, Severity.RED, law),
                rule("IMG_LAYER", fact, Severity.RED, law),
                rule("CAP_FACT", fact, Severity.RED, law),
                rule("CAP_MEDICINE", "식품표시광고법 제8조①2호", Severity.RED, law, "디톡스")));
        var pipeline = new RuleEngineCheckDecisionPipeline(new RuleEngine(), new RegenPromptBuilder(), repo);
        var comparison = new ComparisonService();

        ExtractedAttrs real = attrs(4, 8);
        ExtractedAttrs ai = attrs(9, 12, "블루베리");
        String caption = "12겹 크레이프, 디톡스 딸기";
        var claims = new CaptionClaims(List.of(), 12, null, List.of("디톡스"));

        var result = pipeline.evaluate(new CheckDecisionPipeline.Input(LocalDate.of(2026, 11, 14),
                comparison.compare(ai, real, null), comparison.compare(ai, real, null),
                claims, real, ai, ai, caption));

        assertThat(result.overall()).isEqualTo(Severity.RED);
        assertThat(result.findings()).filteredOn(f -> f.severity() == Severity.RED)
                .extracting(f -> f.ruleCode())
                .contains("IMG_COUNT", "IMG_EXTRA_ITEM", "IMG_LAYER", "CAP_FACT", "CAP_MEDICINE");
        assertThat(result.findings()).filteredOn(f -> f.severity() == Severity.RED)
                .allSatisfy(f -> {
                    assertThat(f.evidence()).isNotBlank();
                    assertThat(f.lawRef()).isNotBlank();
                });
        assertThat(result.regenPrompt()).isNotNull();
        assertThat(result.regenPrompt().ko()).contains("넣지 말 것", "블루베리");
        // #48: 걸린 표현("디톡스")만 지우지 않고 그 표현이 든 구절("디톡스 딸기")을 통째로 지운다
        assertThat(result.captionFix()).isEqualTo("8겹 크레이프 (사진은 연출된 이미지입니다)");
    }

    @Test
    @DisplayName("실물과 같은 사진·문제 없는 문구 → GREEN, 프롬프트 null, 문구 그대로")
    void greenWhenNothingWrong() {
        RuleRepository repo = mock(RuleRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                rule("IMG_COUNT", "식품표시광고법 제8조①4호", Severity.RED, LocalDate.of(2025, 9, 19))));
        var pipeline = new RuleEngineCheckDecisionPipeline(new RuleEngine(), new RegenPromptBuilder(), repo);
        var comparison = new ComparisonService();
        ExtractedAttrs same = attrs(4, 8);
        String caption = "딸기 4개 올린 케이크 (사진은 연출된 이미지입니다)";

        var result = pipeline.evaluate(new CheckDecisionPipeline.Input(LocalDate.of(2026, 11, 14),
                comparison.compare(same, same, null), comparison.compare(same, same, null),
                new CaptionClaims(List.of(), null, null, List.of("연출된 이미지입니다")), same, same, same, caption));

        assertThat(result.overall()).isEqualTo(Severity.GREEN);
        assertThat(result.regenPrompt()).isNull();
        assertThat(result.captionFix()).isEqualTo(caption);
    }
}
