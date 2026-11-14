package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.RegenPrompt;
import com.team.ollyeodo.domain.Severity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 개선 프롬프트(#10). 판정이 모두 끝난 뒤 마지막에 만든다 (MVP §10).
 * <ul>
 *   <li>내용은 AI가 지어내지 않고 <b>코드가 템플릿에 채운다</b> → 같은 판정이면 항상 같은 결과</li>
 *   <li>"넣을 것" = 실물 사진 추출값, "넣지 말 것" = 🔴 항목만 (🟢·checkOnce 항목은 넣지 않음)</li>
 *   <li>종합 🟢면 프롬프트는 null, 문구는 원래 그대로</li>
 * </ul>
 */
@Service
public class RegenPromptBuilder {

    static final String STAGED_NOTICE = "(사진은 연출된 이미지입니다)";

    /** 영어 프롬프트용 재료명 사전. 없으면 한글 이름을 그대로 쓴다. */
    private static final Map<String, String> EN = Map.ofEntries(
            Map.entry("딸기", "strawberry"), Map.entry("블루베리", "blueberry"), Map.entry("라즈베리", "raspberry"),
            Map.entry("생크림", "whipped cream"), Map.entry("휘핑크림", "whipped cream"), Map.entry("크림", "cream"),
            Map.entry("민트", "mint"), Map.entry("민트 잎", "mint leaf"), Map.entry("초콜릿", "chocolate"),
            Map.entry("떡", "rice cake"), Map.entry("닭강정", "sweet crispy fried chicken"), Map.entry("치킨", "fried chicken"),
            Map.entry("소스", "sauce"), Map.entry("피클", "pickle"), Map.entry("크레이프", "crepe"),
            Map.entry("케이크", "cake"), Map.entry("바나나", "banana"), Map.entry("망고", "mango"));

    public RegenOutput build(JudgeResult result, ExtractedAttrs realFirst, String caption) {
        Objects.requireNonNull(result, "result");
        String original = caption == null ? "" : caption;
        if (result.overall() != Severity.RED) {
            return new RegenOutput(null, original);
        }
        List<JudgedFinding> red = result.findings() == null ? List.of()
                : result.findings().stream().filter(f -> f != null && f.severity() == Severity.RED).toList();
        return new RegenOutput(new RegenPrompt(korean(red, realFirst), english(red, realFirst)), captionFix(red, original));
    }

    // ───────── 한국어 프롬프트 ─────────

    private String korean(List<JudgedFinding> red, ExtractedAttrs real) {
        List<String> keep = new ArrayList<>();
        if (real != null && real.items() != null) {
            for (var item : real.items()) {
                if (item == null || item.name() == null || item.name().isBlank()) continue;
                keep.add(item.count() != null ? item.name().strip() + " " + item.count() + "개" : item.name().strip());
            }
        }
        if (real != null && real.layers() != null) keep.add(real.layers() + "겹");
        if (real != null && real.plate() != null && !real.plate().isBlank()) keep.add(real.plate().strip());
        if (real != null && real.garnish() != null) {
            real.garnish().stream().filter(g -> g != null && !g.isBlank()).map(String::strip).forEach(keep::add);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("실제 매장 메뉴 사진과 같은 구성의 음식 사진. 위에서 45도, 자연광.");
        if (!keep.isEmpty()) sb.append("\n넣을 것: ").append(String.join(", ", keep)).append('.');
        sb.append("\n실제 매장 사진과 같은 크기와 양.");
        Set<String> avoid = avoidKo(red);
        if (!avoid.isEmpty()) sb.append("\n넣지 말 것: ").append(String.join(", ", avoid)).append('.');
        return sb.toString();
    }

    private Set<String> avoidKo(List<JudgedFinding> red) {
        Set<String> out = new LinkedHashSet<>();
        for (var f : red) {
            String ai = value(f.aiValue());
            String real = value(f.realValue());
            switch (f.ruleCode()) {
                case "IMG_EXTRA_ITEM" -> out.add(ai.isEmpty() ? "실물에 없는 재료" : ai);
                case "IMG_COUNT" -> out.add(real.isEmpty() ? "실물보다 많은 개수" : "실물(" + real + ")보다 많은 개수");
                case "IMG_FILL" -> out.add("실물보다 많은 양, 접시를 넘치게 담기");
                case "IMG_LAYER" -> out.add(real.isEmpty() ? "실물과 다른 층 수" : "실물(" + real + ")과 다른 층 수");
                case "AI_VIRTUAL_EXPERT" -> out.add("사람(의사·약사 등 전문가), 직함 글자");
                default -> { /* CAP_* 문구 항목은 사진이 아니라 문구 수정안에서 고친다 */ }
            }
        }
        return out;
    }

    // ───────── 영어 프롬프트 ─────────

    private String english(List<JudgedFinding> red, ExtractedAttrs real) {
        List<String> keep = new ArrayList<>();
        if (real != null && real.items() != null) {
            for (var item : real.items()) {
                if (item == null || item.name() == null || item.name().isBlank()) continue;
                String name = en(item.name());
                keep.add(item.count() != null ? item.count() + " " + name : name);
            }
        }
        if (real != null && real.layers() != null) keep.add(real.layers() + " layers");

        StringBuilder sb = new StringBuilder();
        sb.append("A food photo with the same composition as the real store menu photo. 45-degree top view, natural light.");
        if (!keep.isEmpty()) sb.append("\nInclude: ").append(String.join(", ", keep)).append('.');
        sb.append("\nSame size and portion as the real store photo.");
        Set<String> avoid = new LinkedHashSet<>();
        for (var f : red) {
            String ai = value(f.aiValue());
            switch (f.ruleCode()) {
                case "IMG_EXTRA_ITEM" -> avoid.add(ai.isEmpty() ? "ingredients not in the real dish" : en(ai));
                case "IMG_COUNT" -> avoid.add("more pieces than the real dish");
                case "IMG_FILL" -> avoid.add("larger portion than the real dish, overflowing the plate");
                case "IMG_LAYER" -> avoid.add("a different number of layers than the real dish");
                case "AI_VIRTUAL_EXPERT" -> avoid.add("people (doctors, pharmacists or other experts), title text");
                default -> { }
            }
        }
        if (!avoid.isEmpty()) sb.append("\nDo not include: ").append(String.join(", ", avoid)).append('.');
        return sb.toString();
    }

    private static String en(String ko) {
        String key = ko.strip();
        return EN.getOrDefault(key, key);
    }

    // ───────── 문구 수정안 ─────────

    /** 🔴 문구 표현을 지우거나(CAP_*) 실물 값으로 바꾸고(CAP_FACT), "연출 이미지" 안내를 붙인다. */
    private String captionFix(List<JudgedFinding> red, String caption) {
        String fixed = caption;
        for (var f : red) {
            if (!f.ruleCode().startsWith("CAP_")) continue;
            String ai = value(f.aiValue());
            if (ai.isEmpty() || !fixed.contains(ai)) continue;
            String real = value(f.realValue());
            fixed = "CAP_FACT".equals(f.ruleCode()) && !real.isEmpty()
                    ? fixed.replace(ai, real)
                    : fixed.replace(ai, "");
        }
        fixed = tidy(fixed);
        if (!fixed.contains("연출된 이미지")) {
            fixed = fixed.isEmpty() ? STAGED_NOTICE : fixed + " " + STAGED_NOTICE;
        }
        return fixed;
    }

    /** 표현을 지운 뒤 남는 빈칸·쉼표·느낌표를 정리한다. */
    static String tidy(String s) {
        return s.replaceAll("[ \\t]{2,}", " ")
                .replaceAll(" +([,.!?])", "$1")
                .replaceAll("([,.!?])(\\s*[,.!?])+", "$1")
                .replaceAll("^[\\s,.!?]+", "")
                .replaceAll("[\\s,]+$", "")
                .strip();
    }

    private static String value(String v) {
        return v == null ? "" : v.strip();
    }
}
