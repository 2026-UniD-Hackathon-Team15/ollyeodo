package com.team.ollyeodo.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 원본 두 결과를 유지한다. 불일치/확인 불가 필드는 후속 Java 판정에서 처리한다. */
public record ImageExtractionResult(ExtractedAttrs first, ExtractedAttrs second, Agreement agreement) {
    public record Agreement(boolean items, boolean layers, boolean fillRatio, boolean plate,
                            boolean garnish, boolean people, boolean visibleText) {}

    public static ImageExtractionResult compare(ExtractedAttrs a, ExtractedAttrs b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return new ImageExtractionResult(a, b, new Agreement(
                sameList(a.items(), b.items()), same(a.layers(), b.layers()),
                same(a.fillRatio(), b.fillRatio()), same(a.plate(), b.plate()),
                sameList(a.garnish(), b.garnish()), sameList(a.people(), b.people()),
                sameList(a.visibleText(), b.visibleText())));
    }

    private static boolean same(Object a, Object b) {
        return a != null && b != null && a.equals(b);
    }

    // 순서는 무시하되 중복 수는 보존한다. 중첩 record에 null이 있으면 확인 불가다.
    private static boolean sameList(List<?> a, List<?> b) {
        return a != null && b != null && a.stream().allMatch(ImageExtractionResult::known)
                && b.stream().allMatch(ImageExtractionResult::known) && counts(a).equals(counts(b));
    }

    private static boolean known(Object value) {
        if (value instanceof ExtractedAttrs.Item i) {
            return i.name() != null && i.count() != null && i.position() != null;
        }
        if (value instanceof ExtractedAttrs.Person p) {
            return p.attire() != null && p.titleText() != null;
        }
        return value != null;
    }

    private static Map<Object, Integer> counts(List<?> values) {
        Map<Object, Integer> counts = new HashMap<>();
        values.forEach(v -> counts.merge(v, 1, Integer::sum));
        return counts;
    }
}
