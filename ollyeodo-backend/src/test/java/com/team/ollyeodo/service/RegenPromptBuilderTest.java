package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** MVP §7.3 필수 테스트: 🔴 항목이 "넣지 말 것"에 빠짐없이 들어가는지. #9 없이 JudgeResult를 직접 만들어 검증한다. */
class RegenPromptBuilderTest {

    private final RegenPromptBuilder builder = new RegenPromptBuilder();

    private static final ExtractedAttrs REAL = new ExtractedAttrs(
            List.of(new ExtractedAttrs.Item("딸기", 4, "윗면"), new ExtractedAttrs.Item("생크림", null, "사이")),
            8, 0.6, "흰 원형 접시", List.of(), List.of(), List.of());

    private static JudgedFinding red(String code, String ai, String real) {
        return new JudgedFinding(code, Severity.RED, ai, real, "사유", "근거 조항", false);
    }

    private static JudgedFinding green(String code, String ai, boolean checkOnce) {
        return new JudgedFinding(code, Severity.GREEN, ai, null, "안내", null, checkOnce);
    }

    private static String avoidLine(String prompt, String label) {
        return prompt.lines().filter(l -> l.startsWith(label)).findFirst().orElse("");
    }

    @Test
    @DisplayName("🟢면 프롬프트는 null, 문구는 원래 그대로")
    void greenReturnsNoPromptAndOriginalCaption() {
        var out = builder.build(new JudgeResult(Severity.GREEN, List.of(green("CAP_ORIGIN", "국내산 딸기", true))),
                REAL, "국내산 딸기 케이크");
        assertThat(out.regenPrompt()).isNull();
        assertThat(out.captionFix()).isEqualTo("국내산 딸기 케이크");
    }

    @Test
    @DisplayName("🔴 이미지 항목은 모두 '넣지 말 것'에 들어가고, '넣을 것'은 실물 값")
    void everyRedImageFindingIsInAvoidList() {
        var result = new JudgeResult(Severity.RED, List.of(
                red("IMG_EXTRA_ITEM", "블루베리", "없음"),
                red("IMG_COUNT", "딸기 9개", "딸기 4개"),
                red("IMG_FILL", "접시 대비 0.95", "접시 대비 0.6"),
                red("IMG_LAYER", "12겹", "8겹"),
                red("AI_VIRTUAL_EXPERT", "약사", null)));
        var ko = builder.build(result, REAL, "케이크").regenPrompt().ko();

        assertThat(avoidLine(ko, "넣을 것")).contains("딸기 4개", "생크림", "8겹", "흰 원형 접시");
        assertThat(avoidLine(ko, "넣지 말 것")).contains(
                "블루베리", "실물(딸기 4개)보다 많은 개수", "접시를 넘치게", "실물(8겹)과 다른 층 수", "전문가");

        var en = builder.build(result, REAL, "케이크").regenPrompt().en();
        assertThat(avoidLine(en, "Include")).contains("4 strawberry", "whipped cream", "8 layers");
        assertThat(avoidLine(en, "Do not include")).contains("blueberry", "more pieces", "overflowing", "layers", "pharmacists");
    }

    @Test
    @DisplayName("🟢·checkOnce 항목은 '넣지 말 것'에 넣지 않는다 (🔴만)")
    void greenAndCheckOnceAreNotAvoided() {
        var result = new JudgeResult(Severity.RED, List.of(
                red("IMG_EXTRA_ITEM", "블루베리", "없음"),
                green("IMG_COUNT", "딸기 5개", false),
                green("AI_VIRTUAL_EXPERT", "약사", true)));
        var ko = builder.build(result, REAL, "케이크").regenPrompt().ko();
        String avoid = avoidLine(ko, "넣지 말 것");
        assertThat(avoid).contains("블루베리");
        assertThat(avoid).doesNotContain("많은 개수", "전문가");
    }

    @Test
    @DisplayName("문구 수정안: 🔴 표현이 든 구절은 통째로 지우고, CAP_FACT는 실물 값으로 바꾸고, 연출 이미지 안내를 붙인다")
    void captionFixRemovesAndReplaces() {
        var result = new JudgeResult(Severity.RED, List.of(
                red("CAP_FACT", "12겹", "8겹"),
                red("CAP_MEDICINE", "디톡스", null),
                green("CAP_ORIGIN", "국내산", true)));
        var out = builder.build(result, REAL, "12겹 크레이프, 디톡스 딸기! 국내산 딸기");
        assertThat(out.captionFix()).isEqualTo("8겹 크레이프, 국내산 딸기 (사진은 연출된 이미지입니다)");
    }

    @Test
    @DisplayName("문구 수정안: 표현만 지워 어색해지던 문장(…100%. 에도 좋은…)을 구절째 지워 자연스럽게 만든다 (#48)")
    void captionFixDropsWholeSentence() {
        var result = new JudgeResult(Severity.RED, List.of(red("CAP_HFOOD", "다이어트", null)));
        var out = builder.build(result, REAL, "바삭한 닭강정, 손님 만족 100%. 다이어트에도 좋은 건강 간식");
        assertThat(out.captionFix()).isEqualTo("바삭한 닭강정, 손님 만족 100%. (사진은 연출된 이미지입니다)");
    }

    @Test
    @DisplayName("문구 수정안: 숫자 안의 구두점(1.5배, 1,000원)에서는 구절을 자르지 않는다")
    void captionFixKeepsNumbers() {
        var result = new JudgeResult(Severity.RED, List.of(red("CAP_MEDICINE", "디톡스", null)));
        var out = builder.build(result, REAL, "1.5배 큰 딸기 1,000원, 디톡스 효과까지");
        assertThat(out.captionFix()).isEqualTo("1.5배 큰 딸기 1,000원 (사진은 연출된 이미지입니다)");
    }

    @Test
    @DisplayName("영어 프롬프트: 1층이면 '1 layer'(단수), 여러 층이면 'layers'")
    void englishLayerSingular() {
        var oneLayer = new ExtractedAttrs(List.of(new ExtractedAttrs.Item("딸기", 4, "윗면")),
                1, 0.6, null, List.of(), List.of(), List.of());
        var en = builder.build(new JudgeResult(Severity.RED, List.of(red("IMG_EXTRA_ITEM", "블루베리", "없음"))),
                oneLayer, "케이크").regenPrompt().en();
        assertThat(avoidLine(en, "Include")).contains("1 layer").doesNotContain("1 layers");
    }

    @Test
    @DisplayName("이미 연출 이미지 안내가 있으면 다시 붙이지 않고, 같은 입력이면 항상 같은 결과")
    void deterministicAndNoDuplicateNotice() {
        var result = new JudgeResult(Severity.RED, List.of(red("CAP_HFOOD", "혈당 걱정 없는", null)));
        String caption = "혈당 걱정 없는 케이크 (사진은 연출된 이미지입니다)";
        var a = builder.build(result, REAL, caption);
        var b = builder.build(result, REAL, caption);
        assertThat(a).isEqualTo(b);
        assertThat(a.captionFix()).isEqualTo("케이크 (사진은 연출된 이미지입니다)");
    }

    @Test
    @DisplayName("실물 값이 비어 있어도 깨지지 않는다")
    void handlesMissingRealAttrs() {
        var out = builder.build(new JudgeResult(Severity.RED, List.of(red("IMG_EXTRA_ITEM", "블루베리", "없음"))), null, null);
        assertThat(out.regenPrompt().ko()).contains("넣지 말 것: 블루베리").doesNotContain("넣을 것");
        assertThat(out.captionFix()).isEqualTo("(사진은 연출된 이미지입니다)");
    }
}
