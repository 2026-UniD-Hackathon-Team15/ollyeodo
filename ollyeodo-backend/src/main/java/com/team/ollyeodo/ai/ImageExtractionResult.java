package com.team.ollyeodo.ai;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

/** 원본의 plate/garnish는 참고값으로만 보존하며 자기일관성 판정에는 사용하지 않는다. */
public record ImageExtractionResult(ExtractedAttrs first, ExtractedAttrs second,
                                    Agreement agreement) {
    /** MVP에 추출 오차 기준이 없다. 승인 전까지 값이 다른 fill_ratio는 확정 비교하지 않는다. */
    public static final OptionalDouble FILL_RATIO_TOLERANCE = OptionalDouble.empty();

    public enum FillAgreement { MATCH, MISMATCH, TOLERANCE_UNSET, UNKNOWN }
    public record ItemAgreement(Integer firstCount, Integer secondCount, boolean matches) {}
    public record Agreement(Map<String, ItemAgreement> items, boolean layers, FillAgreement fillRatio,
                            boolean people, boolean visibleText) {}
    public static ImageExtractionResult compare(ExtractedAttrs a, ExtractedAttrs b) {
        return compare(a, b, FILL_RATIO_TOLERANCE);
    }

    public static ImageExtractionResult compare(ExtractedAttrs a, ExtractedAttrs b,
                                                OptionalDouble tolerance) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        Set<String> names = new LinkedHashSet<>();
        addNames(names, a.items());
        addNames(names, b.items());
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
                sameList(a.people(), b.people()), sameList(a.visibleText(), b.visibleText())));
    }

    private static void addNames(Set<String> names, List<ExtractedAttrs.Item> items) {
        if (items == null) return;
        items.stream().filter(Objects::nonNull).map(ExtractedAttrs.Item::name)
                .filter(Objects::nonNull).forEach(names::add);
    }

    private static Integer count(List<ExtractedAttrs.Item> items, String name) {
        if (items == null) return null;
        var matches = items.stream().filter(Objects::nonNull).filter(i -> name.equals(i.name())).toList();
        // 누락이나 중복 응답을 0으로 추정하거나 합산하지 않는다.
        if (matches.size() != 1) return null;
        Integer count = matches.getFirst().count();
        return count != null && count >= 0 ? count : null;
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
