package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.CaptionClaims;
import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Rule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 문구 검수 (§6.2) — #9 내부용. 바깥에는 RuleEngine.judge만 노출한다.
 * 검수 대상: 메뉴 설명(홍보 문구)에서 뽑은 CaptionClaims, 비교 기준: 실물 사진 추출값.
 * 여기서는 "걸린 항목"만 찾고, 심각도·asOf·lawRef는 RuleEngine이 정한다.
 */
class CaptionService {

    static final String ORIGIN_CHECK = "원산지 표기가 실제와 같은지 직접 확인해 주세요";
    static final String AI_LABEL_TIP = "'연출된 이미지입니다' 문구를 함께 적는 것을 권장해요";

    /** 키워드 사전으로 검사하지 않는 규칙 (따로 판정) */
    private static final Set<String> NON_KEYWORD_RULES =
            Set.of("CAP_FACT", "CAP_ORIGIN", "AI_VIRTUAL_EXPERT", "INFO_AI_LABEL");

    /** 전문가로 보는 직함 (이미지 직함 글자, 문구의 "○○ 추천") */
    static final List<String> EXPERT_TITLES =
            List.of("의사", "약사", "한의사", "전문의", "교수", "박사", "영양사");

    /**
     * 문구에서 걸린 항목.
     * forceGreen=true면 규칙 심각도와 상관없이 🟢 (CAP_ORIGIN, INFO_AI_LABEL)
     */
    record CaptionHit(String ruleCode, String aiValue, String realValue, String evidence,
                      boolean forceGreen, boolean checkOnce) {

        static CaptionHit of(String ruleCode, String aiValue, String realValue, String evidence) {
            return new CaptionHit(ruleCode, aiValue, realValue, evidence, false, false);
        }
    }

    List<CaptionHit> review(CaptionClaims claims, ExtractedAttrs realFirst,
                            ExtractedAttrs aiFirst, ExtractedAttrs aiSecond, List<Rule> rules) {
        List<CaptionHit> hits = new ArrayList<>();
        List<String> expressions = claims == null || claims.expressions() == null
                ? List.of() : claims.expressions();
        String text = normalize(String.join(" ", expressions));

        if (claims != null && realFirst != null) {
            // 1) CAP_FACT: 층 수
            if (claims.layers() != null && realFirst.layers() != null
                    && !claims.layers().equals(realFirst.layers())) {
                hits.add(CaptionHit.of("CAP_FACT",
                        "문구: " + claims.layers() + "겹", "실물: " + realFirst.layers() + "겹",
                        "메뉴 설명의 층 수가 실물 사진과 다름"));
            }
            // 1) CAP_FACT: 재료 개수
            if (claims.ingredients() != null) {
                for (CaptionClaims.IngredientClaim i : claims.ingredients()) {
                    if (i == null || i.count() == null) continue;
                    Integer realCount = countOf(realFirst, i.name());
                    if (realCount != null && !realCount.equals(i.count())) {
                        hits.add(CaptionHit.of("CAP_FACT",
                                "문구: " + i.name() + " " + i.count() + "개",
                                "실물: " + i.name() + " " + realCount + "개",
                                "메뉴 설명의 재료 개수가 실물 사진과 다름"));
                    }
                }
            }
        }

        // 2) CAP_ORIGIN: 원산지 주장이 있으면 🟢 + 직접 확인 안내 (사진으로는 사실 확인 불가)
        if (claims != null && claims.ingredients() != null) {
            for (CaptionClaims.IngredientClaim i : claims.ingredients()) {
                if (i == null || i.origin() == null || i.origin().isBlank()) continue;
                String name = i.name() == null ? "" : " " + i.name();
                hits.add(new CaptionHit("CAP_ORIGIN", i.origin() + name, null,
                        ORIGIN_CHECK, true, true));
            }
        }

        // 3) 키워드 규칙 (CAP_DISEASE, CAP_MEDICINE, CAP_HFOOD, CAP_SUPERLATIVE, CAP_FAKE_REVIEW)
        if (rules != null) {
            for (Rule rule : rules) {
                if (rule == null || NON_KEYWORD_RULES.contains(rule.getCode())
                        || rule.getKeywords() == null) continue;
                List<String> matched = rule.getKeywords().stream()
                        .filter(k -> k != null && !k.isBlank() && text.contains(normalize(k)))
                        .toList();
                if (!matched.isEmpty()) {
                    hits.add(CaptionHit.of(rule.getCode(), String.join(", ", matched), null,
                            "'" + matched.get(0) + "' — " + rule.getTitle() + " 표현"));
                }
            }
        }

        // 4) AI_VIRTUAL_EXPERT: 이미지 속 전문가(2회 모두, §6.3-2) 또는 문구의 전문가 추천
        Rule expert = rules == null ? null : rules.stream()
                                             .filter(r -> r != null && "AI_VIRTUAL_EXPERT".equals(r.getCode()))
                                             .findFirst().orElse(null);
        boolean inImage = hasExpert(aiFirst) && (aiSecond == null || hasExpert(aiSecond));
        boolean inCaption = (expert != null && containsAny(text, expert.getKeywords()))
                || EXPERT_TITLES.stream().anyMatch(t -> text.contains(t + "추천"));
        if (inImage || inCaption) {
            hits.add(CaptionHit.of("AI_VIRTUAL_EXPERT",
                    inImage ? "이미지 속 전문가로 보이는 인물" : "메뉴 설명의 전문가 추천 표현", null,
                    "AI로 만든 전문가가 추천하는 것처럼 보일 수 있음"));
        }

        // 5) INFO_AI_LABEL: 판정 아님. "연출" 문구가 없으면 권장 안내
        //    (주의 문구가 있어도 판정은 바뀌지 않음, §6.3-1)
        if (!text.contains("연출")) {
            hits.add(new CaptionHit("INFO_AI_LABEL", null, null, AI_LABEL_TIP, true, true));
        }

        return hits;
    }

    /** 실물 사진에서 같은 이름 재료의 개수 */
    private static Integer countOf(ExtractedAttrs attrs, String name) {
        if (attrs.items() == null || name == null) return null;
        String key = normalize(name);
        return attrs.items().stream()
                .filter(it -> it != null && it.name() != null && normalize(it.name()).equals(key))
                .map(ExtractedAttrs.Item::count)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    /** 이미지 속 전문가 단서: 가운, 직함 글자, 이미지 안의 "○○ 추천" 글자 */
    private static boolean hasExpert(ExtractedAttrs attrs) {
        if (attrs == null) return false;
        boolean person = attrs.people() != null && attrs.people().stream().anyMatch(p -> p != null
                && ((p.attire() != null && p.attire().contains("가운"))
                || (p.titleText() != null && EXPERT_TITLES.stream().anyMatch(p.titleText()::contains))));
        boolean visible = attrs.visibleText() != null && attrs.visibleText().stream()
                .filter(Objects::nonNull)
                .map(CaptionService::normalize)
                .anyMatch(t -> EXPERT_TITLES.stream().anyMatch(e -> t.contains(e + "추천")));
        return person || visible;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords != null && keywords.stream()
                .anyMatch(k -> k != null && !k.isBlank() && text.contains(normalize(k)));
    }

    /** 띄어쓰기 차이 무시 ("면역력 강화" = "면역력강화") */
    static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toLowerCase();
    }
}