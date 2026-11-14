package com.team.ollyeodo.service;

import com.team.ollyeodo.ai.ExtractedAttrs;
import com.team.ollyeodo.domain.Severity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * AI 이미지 속성과 실물 기준값을 순수 Java로 비교해 이미지 항목을 판정한다.
 * 기준값은 OLLYEODO_MVP.md §6.1 판정 기준표를 따르며 임의로 바꾸지 않는다.
 * AI는 값만 읽어오고, severity는 이 클래스가 정한다.
 */
@Service
public class ComparisonService {

    // §6.1 판정 기준 (임의 변경 금지)
    static final double YELLOW_RATIO = 1.2;
    static final double RED_RATIO = 1.5;
    private static final double EPS = 1e-9;   // 1.2 같은 경계값이 소수점 오차로 빠지지 않게

    public static final String NO_REAL_PHOTO_NOTICE = "실물 사진을 올리면 더 정확해요";

    /**
     * @param ai    AI 이미지에서 추출한 속성
     * @param real  실물 사진에서 추출한 속성 (실물 사진이 없으면 null)
     * @param facts 메뉴 정보(재료·개수·층 수)를 같은 형식으로 바꾼 값 (#11에서 Menu.facts를 변환해 넘김)
     */
    public ComparisonResult compare(ExtractedAttrs ai, ExtractedAttrs real, ExtractedAttrs facts) {
        List<FindingCandidate> findings = new ArrayList<>();
        List<String> notices = new ArrayList<>();

        // 실물 사진이 없으면 메뉴 facts로 비교하고 안내
        boolean hasReal = real != null;
        String refLabel = hasReal ? "실물" : "메뉴 정보";
        if (!hasReal) {
            notices.add(NO_REAL_PHOTO_NOTICE);
        }
        if (ai == null || (real == null && facts == null)) {
            return new ComparisonResult(findings, notices);
        }

        // 기준 재료: 메뉴 정보 위에 실물 사진 값을 덮어씀 (실물 사진 값 우선)
        Map<String, ExtractedAttrs.Item> refItems = toItemMap(facts);
        refItems.putAll(toItemMap(real));
        Map<String, ExtractedAttrs.Item> aiItems = toItemMap(ai);

        // IMG_EXTRA_ITEM: AI 이미지에만 있는 재료 → 1개 이상이면 🔴
        for (Map.Entry<String, ExtractedAttrs.Item> e : aiItems.entrySet()) {
            if (!refItems.containsKey(e.getKey())) {
                String name = e.getValue().name();
                findings.add(new FindingCandidate(
                        "IMG_EXTRA_ITEM", Severity.RED,
                        name, "없음",
                        refLabel + "에 없는 재료(" + name + ")"));
            }
        }

        // IMG_COUNT: AI 개수 ÷ 실물 개수 → ≥1.2 🟡, ≥1.5 🔴
        for (Map.Entry<String, ExtractedAttrs.Item> e : aiItems.entrySet()) {
            ExtractedAttrs.Item ref = refItems.get(e.getKey());
            ExtractedAttrs.Item aiItem = e.getValue();
            if (ref == null || aiItem.count() == null || ref.count() == null || ref.count() <= 0) {
                continue;
            }
            double ratio = (double) aiItem.count() / ref.count();
            findings.add(new FindingCandidate(
                    "IMG_COUNT", ratioSeverity(ratio),
                    aiItem.name() + " " + aiItem.count() + "개",
                    ref.name() + " " + ref.count() + "개",
                    refLabel + "의 " + formatRatio(ratio) + "배"));
        }

        // IMG_FILL: fill_ratio 비율 (실물 사진이 있을 때만 비교 가능)
        if (hasReal && ai.fillRatio() != null && real.fillRatio() != null && real.fillRatio() > 0) {
            double ratio = ai.fillRatio() / real.fillRatio();
            findings.add(new FindingCandidate(
                    "IMG_FILL", ratioSeverity(ratio),
                    "접시 대비 " + ai.fillRatio(),
                    "접시 대비 " + real.fillRatio(),
                    refLabel + "의 " + formatRatio(ratio) + "배"));
        }

        // IMG_LAYER: 층 수가 다르면 🔴
        Integer refLayers = (real != null && real.layers() != null) ? real.layers()
                : (facts != null ? facts.layers() : null);
        if (ai.layers() != null && refLayers != null) {
            boolean same = ai.layers().equals(refLayers);
            findings.add(new FindingCandidate(
                    "IMG_LAYER", same ? Severity.GREEN : Severity.RED,
                    ai.layers() + "겹",
                    refLayers + "겹",
                    same ? "층 수 같음" : "층 수 다름 (" + ai.layers() + "겹 vs " + refLayers + "겹)"));
        }

        // 색감·조명·배경은 보정 범위라 비교하지 않는다 (§6.1)
        return new ComparisonResult(findings, notices);
    }

    /** 비율 판정: ≥1.5 🔴, ≥1.2 🟡, 그 외 🟢 */
    static Severity ratioSeverity(double ratio) {
        if (ratio + EPS >= RED_RATIO) return Severity.RED;
        if (ratio + EPS >= YELLOW_RATIO) return Severity.YELLOW;
        return Severity.GREEN;
    }

    /** 2.25 → "2.25", 1.50 → "1.5" */
    static String formatRatio(double ratio) {
        return BigDecimal.valueOf(ratio)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /** 재료 이름을 공백 제거·소문자로 맞춰서 비교 (예: "딸기 " = "딸기") */
    private static Map<String, ExtractedAttrs.Item> toItemMap(ExtractedAttrs attrs) {
        Map<String, ExtractedAttrs.Item> map = new LinkedHashMap<>();
        if (attrs == null || attrs.items() == null) return map;
        for (ExtractedAttrs.Item item : attrs.items()) {
            if (item == null || item.name() == null) continue;
            map.put(item.name().replaceAll("\\s+", "").toLowerCase(), item);
        }
        return map;
    }
}