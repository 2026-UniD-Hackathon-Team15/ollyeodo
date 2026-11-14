package com.team.ollyeodo.ai;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/** 원본의 plate/garnish는 참고값으로만 보존하며 자기일관성 판정에는 사용하지 않는다. */
public record ImageExtractionResult(ExtractedAttrs first, ExtractedAttrs second,
                                    Agreement agreement, ExtraItemCandidates extraItemCandidates) {
    /**
     * 같은 사진 2회 추출의 fill_ratio 일치 허용 오차 (절대 차이 0.05 = 5%p 이하면 일치).
     * PM 확정 2026-11-14, OLLYEODO_MVP.md §6.3. §6.1의 1.2/1.5배 판정 임계값과는 별개다.
     */
    public static final OptionalDouble FILL_RATIO_TOLERANCE = OptionalDouble.of(0.05);

    public enum FillAgreement { MATCH, MISMATCH, TOLERANCE_UNSET, UNKNOWN }
    public record ItemAgreement(Integer firstCount, Integer secondCount, boolean matches) {}
    public record Agreement(Map<String, ItemAgreement> items, boolean layers, FillAgreement fillRatio,
                            boolean people, boolean visibleText) {}
    /** IMG_EXTRA_ITEM에서 두 추출 결과와 실물 결과를 확인할 후보. null 목록은 확인 불가다. */
    public record ExtraItemCandidates(List<ExtractedAttrs.Item> first, List<ExtractedAttrs.Item> second) {}

    public static ImageExtractionResult compare(ExtractedAttrs a, ExtractedAttrs b, List<String> ingredients) {
        return compare(a, b, ingredients, FILL_RATIO_TOLERANCE);
    }

    public static ImageExtractionResult compare(ExtractedAttrs a, ExtractedAttrs b, List<String> ingredients,
                                                OptionalDouble tolerance) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        List<String> names = ingredientNames(ingredients);
        Objects.requireNonNull(tolerance);
        if (tolerance.isPresent() && (!Double.isFinite(tolerance.getAsDouble())
                || tolerance.getAsDouble() < 0 || tolerance.getAsDouble() > 1)) {
            throw new IllegalArgumentException("fill_ratio 허용 오차는 0~1 범위여야 합니다.");
        }
        Map<String, ItemAgreement> items = new LinkedHashMap<>();
        for (String name : names) {
            Integer first = count(a.items(), name);
            Integer second = count(b.items(), name);
            items.put(name, new ItemAgreement(first, second, same(first, second)));
        }
        return new ImageExtractionResult(a, b, new Agreement(Collections.unmodifiableMap(items),
                same(a.layers(), b.layers()), compareFill(a.fillRatio(), b.fillRatio(), tolerance),
                sameList(a.people(), b.people()), sameList(a.visibleText(), b.visibleText())),
                new ExtraItemCandidates(extras(a.items(), names), extras(b.items(), names)));
    }

    static List<String> ingredientNames(List<String> ingredients) {
        List<String> names = List.copyOf(ingredients);
        if (names.stream().anyMatch(String::isBlank) || names.stream().distinct().count() != names.size()) {
            throw new IllegalArgumentException("메뉴 재료명은 비어 있거나 중복될 수 없습니다.");
        }
        return names;
    }

    private static Integer count(List<ExtractedAttrs.Item> items, String name) {
        if (items == null) return null;
        var matches = items.stream().filter(Objects::nonNull).filter(i -> name.equals(i.name())).toList();
        // 누락이나 중복 응답을 0으로 추정하거나 합산하지 않는다.
        if (matches.size() != 1) return null;
        Integer count = matches.getFirst().count();
        return count != null && count >= 0 ? count : null;
    }

    private static List<ExtractedAttrs.Item> extras(List<ExtractedAttrs.Item> items, List<String> names) {
        if (items == null) return null;
        return items.stream().filter(Objects::nonNull)
                .filter(i -> i.name() != null && !names.contains(i.name())).toList();
    }

    private static FillAgreement compareFill(Double a, Double b, OptionalDouble tolerance) {
        if (a == null || b == null || !Double.isFinite(a) || !Double.isFinite(b)
                || a < 0 || a > 1 || b < 0 || b > 1) return FillAgreement.UNKNOWN;
        var difference = BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b)).abs();
        if (difference.signum() == 0) return FillAgreement.MATCH;
        if (tolerance.isEmpty()) return FillAgreement.TOLERANCE_UNSET;
        return difference.compareTo(BigDecimal.valueOf(tolerance.getAsDouble())) <= 0
                ? FillAgreement.MATCH : FillAgreement.MISMATCH;
    }

    private static boolean same(Object a, Object b) {
        return a != null && b != null && a.equals(b);
    }

    private static boolean sameList(List<?> a, List<?> b) {
        return a != null && b != null && a.stream().allMatch(ImageExtractionResult::known)
                && b.stream().allMatch(ImageExtractionResult::known) && counts(a).equals(counts(b));
    }

    private static boolean known(Object value) {
        if (value instanceof ExtractedAttrs.Person p) return p.attire() != null && p.titleText() != null;
        return value != null;
    }

    private static Map<Object, Integer> counts(List<?> values) {
        Map<Object, Integer> counts = new HashMap<>();
        values.forEach(v -> counts.merge(v, 1, Integer::sum));
        return counts;
    }
}
