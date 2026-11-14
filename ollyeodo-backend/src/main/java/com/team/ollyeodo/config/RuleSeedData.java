package com.team.ollyeodo.config;

import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;
import java.util.List;

/** MVP §0, §5, §6 기준. URL은 문서에 있는 주소만 사용한다. */
final class RuleSeedData {
    private RuleSeedData() {}

    private static final String SOURCE = "https://www.law.go.kr";
    private static final LocalDate FOOD_EFFECTIVE = LocalDate.of(2025, 9, 19);
    private static final String FACT_LAW = "식품표시광고법 제8조①4호 · 표시광고법 제3조①1호";

    static List<Rule> rules() {
        return List.of(
                rule("IMG_EXTRA_ITEM", "없는 재료", FACT_LAW, FOOD_EFFECTIVE, Severity.RED),
                rule("IMG_COUNT", "개수", FACT_LAW, FOOD_EFFECTIVE, Severity.RED),
                rule("IMG_FILL", "양, 크기", FACT_LAW, FOOD_EFFECTIVE, Severity.RED),
                rule("IMG_LAYER", "층 수, 구성", FACT_LAW, FOOD_EFFECTIVE, Severity.RED),
                rule("CAP_FACT", "수치·재료 불일치 (실물 사진 추출값 기준)", FACT_LAW, FOOD_EFFECTIVE, Severity.RED),
                rule("CAP_ORIGIN", "원산지 표기가 실제와 같은지 직접 확인해 주세요",
                        "원산지표시법 제5조, 제6조①②", LocalDate.of(2026, 10, 22), Severity.GREEN, "국내산 딸기"),
                rule("CAP_DISEASE", "질병 예방·치료", "식품표시광고법 제8조①1호", FOOD_EFFECTIVE, Severity.RED,
                        "감기 예방", "당뇨에 좋은"),
                rule("CAP_MEDICINE", "의약품 오인", "식품표시광고법 제8조①2호", FOOD_EFFECTIVE, Severity.RED,
                        "디톡스", "해독", "면역력 강화"),
                rule("CAP_HFOOD", "건강기능식품 오인", "식품표시광고법 제8조①3호", FOOD_EFFECTIVE, Severity.RED,
                        "혈당 관리", "기능성 디저트"),
                rule("CAP_SUPERLATIVE", "근거 없는 최상급·비교",
                        "식품표시광고법 제8조①7호 · 표시광고법 제3조①3호", FOOD_EFFECTIVE, Severity.RED,
                        "지역 1위", "어디보다 2배"),
                rule("CAP_FAKE_REVIEW", "꾸민 후기",
                        "식품표시광고법 제8조①5호 · 표시광고법 제3조①2호", FOOD_EFFECTIVE, Severity.RED,
                        "손님 100% 만족"),
                rule("AI_VIRTUAL_EXPERT", "AI 가상 전문가 — 11/27부터 금지",
                        "식품표시광고법 제8조①11호", LocalDate.of(2026, 11, 27), Severity.RED, "약사 추천"),
                rule("INFO_AI_LABEL", "연출 이미지 문구 권장", null, null, Severity.GREEN));
    }

    private static Rule rule(String code, String title, String lawRef, LocalDate from,
                             Severity severity, String... keywords) {
        Rule rule = new Rule();
        rule.setCode(code);
        rule.setTitle(title);
        rule.setLawRef(lawRef);
        // 조항별 직접 링크는 문서에 없다. 안내 카드에는 법적 근거를 부여하지 않는다.
        rule.setSourceUrl(lawRef == null ? null : SOURCE);
        rule.setEffectiveFrom(from);
        rule.setEffectiveTo(null);
        rule.setSeverity(severity);
        rule.setPreEffectiveSeverity(Severity.GREEN);
        rule.setKeywords(List.of(keywords));
        return rule;
    }
}
