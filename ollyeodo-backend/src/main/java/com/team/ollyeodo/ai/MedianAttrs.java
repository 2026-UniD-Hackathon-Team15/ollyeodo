package com.team.ollyeodo.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 같은 사진을 3회 추출한 결과를 하나로 합친다 (MVP §6.3-2). 판정은 하지 않는다.
 * - 개수·층 수·담긴 비율: 중앙값 (한 번 튄 값을 버린다)
 * - 재료가 사진에 있는지·적용 판단·같은 음식 판단: 과반(3회 중 2회 이상)
 */
public final class MedianAttrs {
    private MedianAttrs() {}

    /**
     * @param runs    같은 사진의 추출 결과 (보통 3개)
     * @param keepAll true면 한 번이라도 나온 재료를 모두 남긴다 (실물 사진: 기준 목록이 빠지면
     *                AI 사진 재료가 "없는 재료"로 잘못 걸리므로). false면 과반 회차에 나온 재료만 남긴다.
     */
    public static ExtractedAttrs of(List<ExtractedAttrs> runs, boolean keepAll) {
        List<ExtractedAttrs> rs = runs.stream().filter(Objects::nonNull).toList();
        if (rs.isEmpty()) throw new IllegalArgumentException("추출 결과가 없습니다.");
        if (rs.size() == 1) return rs.getFirst();
        int majority = rs.size() / 2 + 1;
        ExtractedAttrs base = rs.getFirst();

        List<ExtractedAttrs.Item> items = new ArrayList<>();
        for (String name : names(rs)) {
            List<ExtractedAttrs.Item> found = rs.stream().map(r -> find(r, name)).toList();
            long present = found.stream().filter(Objects::nonNull).count();
            if (!keepAll && present < majority) continue;
            // 이번 회차에 없으면 0개로 본다. 셀 수 없는 재료(null)는 값에서 뺀다.
            List<Integer> counts = new ArrayList<>();
            for (ExtractedAttrs.Item it : found) {
                if (it == null) counts.add(0);
                else if (it.count() != null) counts.add(it.count());
            }
            Integer count = counts.size() >= majority ? medianInt(counts) : null;
            ExtractedAttrs.Item sample = found.stream().filter(Objects::nonNull)
                    .filter(it -> Objects.equals(it.count(), count)).findFirst()
                    .orElse(found.stream().filter(Objects::nonNull).findFirst().orElseThrow());
            items.add(new ExtractedAttrs.Item(sample.name(), sample.countNote(), sample.visibleCount(),
                    sample.hiddenEstimate(), count, sample.position()));
        }

        Integer layers = medianInt(rs.stream().map(ExtractedAttrs::layers).filter(Objects::nonNull).toList(), majority);
        Double fill = medianDouble(rs.stream().map(ExtractedAttrs::fillRatio).filter(Objects::nonNull).toList(), majority);
        String foodType = rs.stream().map(ExtractedAttrs::foodType)
                .filter(t -> t != null && !t.isBlank()).findFirst().orElse(null);
        Boolean sameFood = majorityBoolean(rs.stream().map(ExtractedAttrs::sameFood).toList(), majority);

        return new ExtractedAttrs(items, layers, fill, base.plate(), base.garnish(), base.people(),
                base.visibleText(), foodType, applicability(rs, majority), sameFood);
    }

    /** 규칙별로 과반이 false면 false (이유는 처음 false가 나온 회차), 아니면 true */
    private static List<ExtractedAttrs.Applicability> applicability(List<ExtractedAttrs> rs, int majority) {
        Map<String, List<ExtractedAttrs.Applicability>> byCode = new LinkedHashMap<>();
        for (ExtractedAttrs r : rs) {
            if (r.applicability() == null) continue;
            for (ExtractedAttrs.Applicability a : r.applicability()) {
                if (a == null || a.ruleCode() == null) continue;
                byCode.computeIfAbsent(a.ruleCode(), k -> new ArrayList<>()).add(a);
            }
        }
        if (byCode.isEmpty()) return null;
        List<ExtractedAttrs.Applicability> result = new ArrayList<>();
        byCode.forEach((code, list) -> {
            List<ExtractedAttrs.Applicability> no = list.stream()
                    .filter(a -> Boolean.FALSE.equals(a.applicable())).toList();
            if (no.size() >= majority) {
                result.add(new ExtractedAttrs.Applicability(code, false, no.getFirst().reason()));
            } else {
                ExtractedAttrs.Applicability yes = list.stream()
                        .filter(a -> !Boolean.FALSE.equals(a.applicable())).findFirst().orElse(list.getFirst());
                result.add(new ExtractedAttrs.Applicability(code, true, yes.reason()));
            }
        });
        return result;
    }

    private static Boolean majorityBoolean(List<Boolean> values, int majority) {
        long no = values.stream().filter(Boolean.FALSE::equals).count();
        long yes = values.stream().filter(Boolean.TRUE::equals).count();
        if (no >= majority) return false;
        if (yes >= majority) return true;
        return null;
    }

    private static Set<String> names(List<ExtractedAttrs> rs) {
        Set<String> names = new LinkedHashSet<>();
        for (ExtractedAttrs r : rs) {
            if (r.items() == null) continue;
            r.items().stream().filter(Objects::nonNull).map(ExtractedAttrs.Item::name)
                    .filter(n -> n != null && !n.isBlank()).map(String::strip).forEach(names::add);
        }
        return names;
    }

    private static ExtractedAttrs.Item find(ExtractedAttrs r, String name) {
        if (r.items() == null) return null;
        return r.items().stream().filter(i -> i != null && i.name() != null && i.name().strip().equals(name))
                .findFirst().orElse(null);
    }

    private static Integer medianInt(List<Integer> values, int majority) {
        return values.size() >= majority ? medianInt(values) : null;
    }

    static Integer medianInt(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (int) Math.round((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);
    }

    private static Double medianDouble(List<Double> values, int majority) {
        if (values.size() < majority) return null;
        List<Double> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }
}
