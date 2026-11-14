package com.team.ollyeodo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.CaptionReview;
import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** §6.2·§6.3 판정 테스트. 규칙은 DB 없이 인메모리 List<Rule>로 (#5 머지 전) */
class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final LocalDate LAW = LocalDate.of(2025, 9, 19);
    private static final String LAW_IMG = "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호";

    // ---------- 인메모리 규칙 (§6.1·§6.2 표 기준, 13행) ----------
    private static Rule rule(String code, String title, String lawRef, LocalDate from,
                             Severity sev, String... keywords) {
        Rule r = new Rule();
        r.setCode(code);
        r.setTitle(title);
        r.setLawRef(lawRef);
        r.setEffectiveFrom(from);
        r.setSeverity(sev);
        r.setPreEffectiveSeverity(Severity.GREEN);
        r.setKeywords(List.of(keywords));
        return r;
    }

    private final List<Rule> rules = List.of(
            rule("IMG_EXTRA_ITEM", "없는 재료", LAW_IMG, LAW, Severity.RED),
            rule("IMG_COUNT", "개수", LAW_IMG, LAW, Severity.RED),
            rule("IMG_FILL", "양, 크기", LAW_IMG, LAW, Severity.RED),
            rule("IMG_LAYER", "층 수, 구성", LAW_IMG, LAW, Severity.RED),
            rule("CAP_FACT", "수치·재료 불일치", LAW_IMG, LAW, Severity.RED),
            rule("CAP_ORIGIN", "원산지 표기", "원산지표시법 제5조, 제6조①②", LAW, Severity.GREEN),
            rule("CAP_DISEASE", "질병 예방·치료", "식품표시광고법 제8조①1호", LAW, Severity.RED,
                    "감기 예방", "당뇨에 좋은"),
            rule("CAP_MEDICINE", "의약품 오인", "식품표시광고법 제8조①2호", LAW, Severity.RED,
                    "디톡스", "해독", "면역력 강화"),
            rule("CAP_HFOOD", "건강기능식품 오인", "식품표시광고법 제8조①3호", LAW, Severity.RED,
                    "혈당 관리", "기능성 디저트"),
            rule("CAP_SUPERLATIVE", "근거 없는 최상급·비교", "식품표시광고법 제8조①7호", LAW, Severity.RED,
                    "지역 1위", "어디보다"),
            rule("CAP_FAKE_REVIEW", "꾸민 후기", "식품표시광고법 제8조①5호", LAW, Severity.RED,
                    "100% 만족"),
            rule("AI_VIRTUAL_EXPERT", "AI 가상 전문가", "식품표시광고법 제8조①11호",
                    LocalDate.of(2026, 11, 27), Severity.RED, "약사 추천", "의사 추천"),
            rule("INFO_AI_LABEL", "AI 생성 안내", "판정 아님 (§5.2)", LAW, Severity.GREEN)
    );

    // ---------- 도우미 ----------
    private static FindingCandidate fc(String code, Severity s, String ai, String real, String ev) {
        return new FindingCandidate(code, s, ai, real, ev);
    }

    private static ComparisonResult run(FindingCandidate... fs) {
        return new ComparisonResult(List.of(fs), List.of());
    }

    private static CaptionClaims claims(Integer layers, List<CaptionClaims.IngredientClaim> ingredients,
                                        String... expressions) {
        return new CaptionClaims(ingredients, layers, null, List.of(expressions));
    }

    private static ExtractedAttrs real(int layers, int strawberries) {
        return new ExtractedAttrs(List.of(new ExtractedAttrs.Item("딸기", strawberries, "윗면")),
                layers, 0.7, "흰 원형 접시", List.of(), List.of(), List.of());
    }

    private static ExtractedAttrs aiPlain() {
        return new ExtractedAttrs(List.of(), null, null, null, List.of(), List.of(), List.of());
    }

    private static ExtractedAttrs aiPerson(String attire, String title) {
        return new ExtractedAttrs(List.of(), null, null, null, List.of(),
                List.of(new ExtractedAttrs.Person(attire, title)), List.of());
    }

    private JudgeResult judge(LocalDate asOf, ComparisonResult r1, ComparisonResult r2, CaptionClaims c,
                              ExtractedAttrs realFirst, ExtractedAttrs aiFirst, ExtractedAttrs aiSecond) {
        return engine.judge(new JudgeInput(asOf, r1, r2, c, realFirst, aiFirst, aiSecond), rules);
    }

    /** 이미지 비교는 없고 문구만 검사할 때 */
    private JudgeResult judgeCaption(LocalDate asOf, CaptionClaims c) {
        return judge(asOf, run(), run(), c, real(8, 4), aiPlain(), aiPlain());
    }

    private static Optional<JudgedFinding> find(JudgeResult r, String code) {
        return r.findings().stream().filter(f -> f.ruleCode().equals(code)).findFirst();
    }

    // ---------- 테스트 ----------

    @Test
    @DisplayName("'디톡스' → CAP_MEDICINE 🔴 + 근거 조항")
    void medicineKeywordIsRed() {
        JudgeResult r = judgeCaption(TODAY, claims(null, List.of(), "디톡스"));

        JudgedFinding f = find(r, "CAP_MEDICINE").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.lawRef()).isEqualTo("식품표시광고법 제8조①2호");
        assertThat(f.evidence()).contains("디톡스");
        assertThat(r.overall()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("원산지 주장 → CAP_ORIGIN 🟢 + checkOnce + 직접 확인 안내 (판정에 영향 없음)")
    void originIsGreenWithCheckOnce() {
        JudgeResult r = judgeCaption(TODAY, claims(null,
                List.of(new CaptionClaims.IngredientClaim("딸기", null, "국내산")), "국내산 딸기"));

        JudgedFinding f = find(r, "CAP_ORIGIN").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.GREEN);
        assertThat(f.checkOnce()).isTrue();
        assertThat(f.evidence()).isEqualTo(CaptionService.ORIGIN_CHECK);
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("AI_VIRTUAL_EXPERT asOf 11/26 → 🟢 + checkOnce + '11월 27일부터 금지'")
    void expertBeforeEffective() {
        JudgeResult r = judgeCaption(LocalDate.of(2026, 11, 26), claims(null, List.of(), "약사 추천"));

        JudgedFinding f = find(r, "AI_VIRTUAL_EXPERT").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.GREEN);
        assertThat(f.checkOnce()).isTrue();
        assertThat(f.evidence()).contains("11월 27일부터 금지");
    }

    @Test
    @DisplayName("AI_VIRTUAL_EXPERT asOf 11/27 → 🔴")
    void expertOnEffectiveDate() {
        JudgeResult r = judgeCaption(LocalDate.of(2026, 11, 27), claims(null, List.of(), "약사 추천"));

        JudgedFinding f = find(r, "AI_VIRTUAL_EXPERT").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.checkOnce()).isFalse();
        assertThat(f.lawRef()).isEqualTo("식품표시광고법 제8조①11호");
        assertThat(r.overall()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("이미지 속 흰 가운 인물이 2회 모두 보이면 AI_VIRTUAL_EXPERT (11/27 → 🔴)")
    void expertInImageBothRuns() {
        JudgeResult r = judge(LocalDate.of(2026, 11, 27), run(), run(), claims(null, List.of()),
                real(8, 4), aiPerson("흰 가운", "약사"), aiPerson("흰 가운", null));

        assertThat(find(r, "AI_VIRTUAL_EXPERT").orElseThrow().severity()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("이미지 속 전문가가 2회 중 한 번만 보이면 AI_VIRTUAL_EXPERT 아님 (§6.3-2)")
    void expertInImageOneRunOnly() {
        JudgeResult r = judge(LocalDate.of(2026, 11, 27), run(), run(), claims(null, List.of()),
                real(8, 4), aiPerson("흰 가운", "약사"), aiPlain());

        assertThat(find(r, "AI_VIRTUAL_EXPERT")).isEmpty();
    }

    @Test
    @DisplayName("2회 추출 중 한 번만 기준을 넘으면 🟢 (§6.3-2)")
    void imageOneRunOnlyIsGreen() {
        JudgeResult r = judge(TODAY,
                run(fc("IMG_COUNT", Severity.RED, "딸기 9개", "딸기 4개", "실물의 2.25배")),
                run(fc("IMG_COUNT", Severity.GREEN, "딸기 4개", "딸기 4개", "실물의 1배")),
                claims(null, List.of()), real(8, 4), aiPlain(), aiPlain());

        JudgedFinding f = find(r, "IMG_COUNT").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.GREEN);
        assertThat(f.evidence()).contains("한 번만");
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("2회 모두 기준을 넘으면 🔴 + 근거 조항")
    void imageBothRunsIsRed() {
        JudgeResult r = judge(TODAY,
                run(fc("IMG_COUNT", Severity.RED, "딸기 9개", "딸기 4개", "실물의 2.25배")),
                run(fc("IMG_COUNT", Severity.RED, "딸기 8개", "딸기 4개", "실물의 2배")),
                claims(null, List.of()), real(8, 4), aiPlain(), aiPlain());

        JudgedFinding f = find(r, "IMG_COUNT").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.evidence()).isEqualTo("실물의 2.25배");
        assertThat(f.lawRef()).isEqualTo(LAW_IMG);
    }

    @Test
    @DisplayName("#8의 🟡(1.2~1.5배)는 §6.1상 1.5배 미만이라 🟢")
    void yellowFromComparisonIsGreen() {
        JudgeResult r = judge(TODAY,
                run(fc("IMG_FILL", Severity.YELLOW, "접시 대비 0.9", "접시 대비 0.7", "실물의 1.29배")),
                run(fc("IMG_FILL", Severity.YELLOW, "접시 대비 0.9", "접시 대비 0.7", "실물의 1.29배")),
                claims(null, List.of()), real(8, 4), aiPlain(), aiPlain());

        assertThat(find(r, "IMG_FILL").orElseThrow().severity()).isEqualTo(Severity.GREEN);
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("문구 '12겹' vs 실물 8겹 → CAP_FACT 🔴")
    void captionLayerMismatchIsRed() {
        JudgeResult r = judgeCaption(TODAY, claims(12, List.of(), "12겹 크레이프"));

        JudgedFinding f = find(r, "CAP_FACT").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.aiValue()).isEqualTo("12겹");
        assertThat(f.realValue()).isEqualTo("8겹");
    }

    @Test
    @DisplayName("문구 '딸기 10개' vs 실물 4개 → CAP_FACT 🔴")
    void captionCountMismatchIsRed() {
        JudgeResult r = judgeCaption(TODAY, claims(null,
                List.of(new CaptionClaims.IngredientClaim("딸기", 10, null)), "딸기 10개"));

        assertThat(find(r, "CAP_FACT").orElseThrow().severity()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("주의 문구('연출된 이미지')가 있어도 판정은 같다 (§6.3-1)")
    void stagedNoticeDoesNotChangeVerdict() {
        JudgeResult without = judgeCaption(TODAY, claims(null, List.of(), "디톡스"));
        JudgeResult with = judgeCaption(TODAY, claims(null, List.of(), "디톡스", "연출된 이미지입니다"));

        assertThat(find(with, "CAP_MEDICINE").orElseThrow().severity()).isEqualTo(Severity.RED);
        assertThat(with.overall()).isEqualTo(without.overall());
        assertThat(find(with, "INFO_AI_LABEL")).isEmpty();          // 문구가 있으면 권장 안내 안 함
        assertThat(find(without, "INFO_AI_LABEL").orElseThrow().checkOnce()).isTrue();
    }

    @Test
    @DisplayName("종합: 🔴가 없으면 🟢")
    void overallGreenWhenNoRed() {
        JudgeResult r = judgeCaption(TODAY, claims(8, List.of(), "딸기 크레이프 케이크"));

        assertThat(r.overall()).isEqualTo(Severity.GREEN);
        assertThat(r.findings()).allMatch(f -> f.severity() == Severity.GREEN);
    }

    @Test
    @DisplayName("데모: 딸기 2.25배·블루베리·12겹 vs 8겹·'디톡스' → 전부 🔴, 🔴는 evidence·lawRef 필수, 단정 표현 없음")
    void demoScenario() {
        ComparisonResult image = run(
                fc("IMG_EXTRA_ITEM", Severity.RED, "블루베리", "없음", "실물에 없는 재료(블루베리)"),
                fc("IMG_COUNT", Severity.RED, "딸기 9개", "딸기 4개", "실물의 2.25배"),
                fc("IMG_LAYER", Severity.RED, "12겹", "8겹", "층 수 다름 (12겹 vs 8겹)"));
        JudgeResult r = judge(TODAY, image, image,
                claims(12, List.of(), "12겹 크레이프", "디톡스 딸기"),
                real(8, 4), aiPlain(), aiPlain());

        assertThat(r.overall()).isEqualTo(Severity.RED);
        for (String code : List.of("IMG_EXTRA_ITEM", "IMG_COUNT", "IMG_LAYER", "CAP_FACT", "CAP_MEDICINE")) {
            assertThat(find(r, code).orElseThrow().severity()).as(code).isEqualTo(Severity.RED);
        }
        assertThat(r.findings()).filteredOn(f -> f.severity() == Severity.RED)
                .allMatch(f -> f.evidence() != null && !f.evidence().isBlank()
                        && f.lawRef() != null && !f.lawRef().isBlank());
        assertThat(r.findings()).noneMatch(f -> f.evidence() != null
                && (f.evidence().contains("위법") || f.evidence().contains("불법")));
    }

    @Test
    @DisplayName("asOf가 비어 있으면 오늘(Asia/Seoul) 기준으로 판정한다")
    void nullAsOfUsesToday() {
        JudgeResult r = judgeCaption(null, claims(null, List.of(), "디톡스"));

        assertThat(r.overall()).isEqualTo(Severity.RED);
    }

    @Test
    @DisplayName("종료일(effectiveTo)이 지난 규칙은 적용하지 않는다")
    void expiredRuleIgnored() {
        Rule old = rule("CAP_OLD", "옛 규칙", "옛 조항", LocalDate.of(2020, 1, 1), Severity.RED, "옛표현");
        old.setEffectiveTo(LocalDate.of(2025, 12, 31));

        JudgeResult r = engine.judge(new JudgeInput(TODAY, run(), run(),
                claims(null, List.of(), "옛표현"), real(8, 4), aiPlain(), aiPlain()), List.of(old));

        assertThat(find(r, "CAP_OLD")).isEmpty();
    }

    // ---------- §6.1.1 적용 판단 · §6.2 AI 문구 분류 ----------

    private static ExtractedAttrs realWith(Boolean count, Boolean fill, Boolean layer) {
        return new ExtractedAttrs(List.of(new ExtractedAttrs.Item("닭강정", null, "그릇")),
                null, 0.8, "그릇", List.of(), List.of(), List.of(), "닭강정", List.of(
                new ExtractedAttrs.Applicability("IMG_COUNT", count, "수북이 쌓여 셀 수 없음"),
                new ExtractedAttrs.Applicability("IMG_FILL", fill, "그릇이 보임"),
                new ExtractedAttrs.Applicability("IMG_LAYER", layer, "층으로 쌓인 음식이 아님")));
    }

    private static CaptionReview review(CaptionReview.Item... items) {
        return new CaptionReview(List.of(items));
    }

    private static CaptionReview.Item item(String expr, String code) {
        return new CaptionReview.Item(expr, code, null, null);
    }

    private JudgeResult judgeAi(ComparisonResult r1, ComparisonResult r2, ExtractedAttrs realFirst,
                                ExtractedAttrs realSecond, String caption, CaptionReview rv1, CaptionReview rv2) {
        return engine.judge(new JudgeInput(TODAY, r1, r2, claims(null, List.of()), realFirst, aiPlain(), aiPlain(),
                realSecond, caption, rv1, rv2), rules);
    }

    @Test
    @DisplayName("§6.3-5 실물 2회 중 한 번이라도 applicable=false면 비교하지 않고 해당 없음 (종합에 영향 없음)")
    void notApplicableSkipsComparison() {
        var red = run(fc("IMG_LAYER", Severity.RED, "3겹", "1겹", "층 수 다름"));
        JudgeResult r = judgeAi(red, red, realWith(true, true, true), realWith(true, true, false),
                "바삭한 닭강정 (사진은 연출된 이미지입니다)", review(), review());

        JudgedFinding f = find(r, "IMG_LAYER").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.GREEN);
        assertThat(f.evidence()).startsWith("해당 없음").contains("층으로 쌓인 음식이 아님");
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("두 번 모두 applicable=true면 그대로 비교한다")
    void applicableIsCompared() {
        var red = run(fc("IMG_FILL", Severity.RED, "접시 대비 1.0", "실물의 2배", "양 다름"));
        JudgeResult r = judgeAi(red, red, realWith(false, true, false), realWith(false, true, false),
                "닭강정", review(), review());

        assertThat(find(r, "IMG_FILL").orElseThrow().severity()).isEqualTo(Severity.RED);
        assertThat(find(r, "IMG_COUNT").orElseThrow().evidence()).startsWith("해당 없음");
    }

    @Test
    @DisplayName("§6.3-6 문구 분류 2회가 같은 표현·같은 코드면 🔴, 근거 조항은 규칙표에서")
    void classifiedBothRunsIsRed() {
        JudgeResult r = judgeAi(run(), run(), null, null, "속이 편해지는 디톡스 닭강정",
                review(item("디톡스", "CAP_MEDICINE")), review(item("디톡스 닭강정", "CAP_MEDICINE")));

        JudgedFinding f = find(r, "CAP_MEDICINE").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.lawRef()).isEqualTo("식품표시광고법 제8조①2호");
        assertThat(f.evidence()).doesNotContain("간이 검수");
    }

    @Test
    @DisplayName("문구 분류가 한 번만 걸리면 🟢")
    void classifiedOneRunIsGreen() {
        JudgeResult r = judgeAi(run(), run(), null, null, "동네 최고 닭강정",
                review(item("동네 최고", "CAP_SUPERLATIVE")), review());

        assertThat(find(r, "CAP_SUPERLATIVE").orElseThrow().severity()).isEqualTo(Severity.GREEN);
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("§6.3-8 문구에 없는 인용, 표에 없는 코드는 버린다")
    void invalidQuoteOrCodeDiscarded() {
        JudgeResult r = judgeAi(run(), run(), null, null, "맛있는 닭강정",
                review(item("암 예방", "CAP_DISEASE"), item("맛있는", "CAP_TASTY")),
                review(item("암 예방", "CAP_DISEASE"), item("맛있는", "CAP_TASTY")));

        assertThat(find(r, "CAP_DISEASE")).isEmpty();
        assertThat(find(r, "CAP_TASTY")).isEmpty();
        assertThat(r.overall()).isEqualTo(Severity.GREEN);
    }

    @Test
    @DisplayName("CAP_FACT는 claimed ≠ observed일 때만 🔴")
    void classifiedFactNeedsDifference() {
        var diff = new CaptionReview.Item("12겹 크레이프", "CAP_FACT", "12겹", "8겹");
        var same = new CaptionReview.Item("딸기 5개", "CAP_FACT", "5개", "5개");
        JudgeResult r = judgeAi(run(), run(), null, null, "12겹 크레이프, 딸기 5개",
                review(diff, same), review(diff, same));

        var facts = r.findings().stream().filter(f -> f.ruleCode().equals("CAP_FACT")).toList();
        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).severity()).isEqualTo(Severity.RED);
        assertThat(facts.get(0).aiValue()).isEqualTo("12겹");
        assertThat(facts.get(0).realValue()).isEqualTo("8겹");
    }

    @Test
    @DisplayName("§6.3-7 분류 실패(null)면 키워드 간이 검수로 대신하고 표시한다")
    void fallbackIsMarkedSimpleCheck() {
        JudgeResult r = judgeAi(run(), run(), null, null, "속이 편한 디톡스 닭강정", null, null);

        JudgedFinding f = find(r, "CAP_MEDICINE").orElseThrow();
        assertThat(f.severity()).isEqualTo(Severity.RED);
        assertThat(f.evidence()).endsWith("(간이 검수)");
    }
}
