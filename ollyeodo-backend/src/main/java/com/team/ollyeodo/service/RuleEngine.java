package com.team.ollyeodo.service;

import com.team.ollyeodo.domain.Rule;
import com.team.ollyeodo.domain.Severity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 판정 엔진 (§6.3). 판정은 🟢·🔴 2단계. AI는 값만 읽고, 판정은 이 클래스가 한다.
 * - 이미지: run1·run2 둘 다 🔴인 항목만 🔴, 아니면 🟢
 *   (#8 ComparisonService의 1.2~1.5배 🟡는 §6.1상 1.5배 미만이라 🟢로 본다)
 * - effectiveFrom ≤ asOf (그리고 effectiveTo가 없거나 asOf ≤ effectiveTo)인 규칙만 적용
 * - 시행 전 규칙은 🟢 + checkOnce + "○월 ○일부터 금지"
 * - 종합: 🔴가 하나라도 있으면 🔴, 없으면 🟢
 * 위법 여부를 단정하지 않고 "이 조항에 걸릴 수 있는 차이"만 알려준다.
 */
@Service
public class RuleEngine {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    static final String ONE_RUN_ONLY = " (2회 추출 중 한 번만 기준을 넘어 🟢로 둠)";

    private final CaptionService captionService = new CaptionService();

    public JudgeResult judge(JudgeInput input, List<Rule> rules) {
        LocalDate asOf = input.asOf() != null ? input.asOf() : LocalDate.now(SEOUL);
        List<Rule> safeRules = rules == null ? List.of() : rules;
        Map<String, Rule> byCode = byCode(safeRules);

        List<JudgedFinding> findings = new ArrayList<>(
                judgeImages(input.run1(), input.run2(), byCode, asOf));

        for (CaptionService.CaptionHit hit : captionService.review(
                input.captionClaims(), input.realFirst(), input.aiFirst(), input.aiSecond(), safeRules)) {
            JudgedFinding f = apply(hit, byCode.get(hit.ruleCode()), asOf);
            if (f != null) findings.add(f);
        }

        Severity overall = findings.stream().anyMatch(f -> f.severity() == Severity.RED)
                ? Severity.RED : Severity.GREEN;
        return new JudgeResult(overall, findings);
    }

    // ---------- 이미지 (run1 × run2) ----------

    private List<JudgedFinding> judgeImages(ComparisonResult run1, ComparisonResult run2,
                                            Map<String, Rule> byCode, LocalDate asOf) {
        Map<String, FindingCandidate> r1 = byKey(run1);
        Map<String, FindingCandidate> r2 = byKey(run2);
        Set<String> keys = new LinkedHashSet<>(r1.keySet());
        keys.addAll(r2.keySet());

        List<JudgedFinding> result = new ArrayList<>();
        for (String key : keys) {
            FindingCandidate f1 = r1.get(key);
            FindingCandidate f2 = r2.get(key);
            FindingCandidate base = f1 != null ? f1 : f2;
            Rule rule = byCode.get(base.ruleCode());
            if (rule != null && isExpired(rule, asOf)) continue;

            boolean red1 = isRed(f1);
            boolean red2 = isRed(f2);
            boolean red = red1 && red2;                 // §6.3-2: 두 번 모두 넘을 때만 🔴
            String evidence = base.evidence();
            if (!red && (red1 || red2)) {               // 한 번만 넘음 → 🟢 + 사유 표시
                base = red1 ? f1 : f2;
                evidence = base.evidence() + ONE_RUN_ONLY;
            }
            result.add(new JudgedFinding(base.ruleCode(), red ? Severity.RED : Severity.GREEN,
                    base.aiValue(), base.realValue(), evidence,
                    rule == null ? null : rule.getLawRef(), false));
        }
        return result;
    }

    /** 같은 항목끼리 짝짓기 위한 키 (재료별 규칙은 재료 이름까지 포함) */
    private static Map<String, FindingCandidate> byKey(ComparisonResult run) {
        Map<String, FindingCandidate> map = new LinkedHashMap<>();
        if (run == null || run.findings() == null) return map;
        for (FindingCandidate f : run.findings()) {
            if (f == null || f.ruleCode() == null) continue;
            String key = key(f);
            FindingCandidate old = map.get(key);
            if (old == null || (!isRed(old) && isRed(f))) map.put(key, f);
        }
        return map;
    }

    private static String key(FindingCandidate f) {
        String ai = f.aiValue() == null ? "" : f.aiValue();
        return switch (f.ruleCode()) {
            case "IMG_EXTRA_ITEM" -> f.ruleCode() + "|" + CaptionService.normalize(ai);
            case "IMG_COUNT" -> f.ruleCode() + "|" + CaptionService.normalize(ai.replaceAll("\\s*\\d+개$", ""));
            default -> f.ruleCode();
        };
    }

    private static boolean isRed(FindingCandidate f) {
        return f != null && f.severity() == Severity.RED;
    }

    // ---------- 문구 (CaptionHit → 판정) ----------

    private JudgedFinding apply(CaptionService.CaptionHit hit, Rule rule, LocalDate asOf) {
        if (rule != null && isExpired(rule, asOf)) return null;
        String lawRef = rule == null ? null : rule.getLawRef();

        if (hit.forceGreen()) {   // CAP_ORIGIN, INFO_AI_LABEL: 판정에 영향 없음
            return new JudgedFinding(hit.ruleCode(), Severity.GREEN, hit.aiValue(), hit.realValue(),
                    hit.evidence(), lawRef, hit.checkOnce());
        }
        if (rule != null && isPreEffective(rule, asOf)) {   // 시행 전 → 🟢 + 안내
            LocalDate from = rule.getEffectiveFrom();
            String notice = from.getMonthValue() + "월 " + from.getDayOfMonth() + "일부터 금지";
            return new JudgedFinding(hit.ruleCode(), Severity.GREEN, hit.aiValue(), hit.realValue(),
                    hit.evidence() + " — " + notice, lawRef, true);
        }
        // §6.2 문구 규칙은 🔴 (예전 데이터의 YELLOW도 🔴로 맞춤)
        Severity severity = rule != null && rule.getSeverity() == Severity.GREEN
                ? Severity.GREEN : Severity.RED;
        return new JudgedFinding(hit.ruleCode(), severity, hit.aiValue(), hit.realValue(),
                hit.evidence(), lawRef, false);
    }

    // ---------- 시행일 ----------

    static boolean isPreEffective(Rule rule, LocalDate asOf) {
        return rule.getEffectiveFrom() != null && asOf.isBefore(rule.getEffectiveFrom());
    }

    static boolean isExpired(Rule rule, LocalDate asOf) {
        return rule.getEffectiveTo() != null && asOf.isAfter(rule.getEffectiveTo());
    }

    private static Map<String, Rule> byCode(List<Rule> rules) {
        Map<String, Rule> map = new LinkedHashMap<>();
        for (Rule r : rules) {
            if (r != null && r.getCode() != null) map.put(r.getCode(), r);
        }
        return map;
    }
}