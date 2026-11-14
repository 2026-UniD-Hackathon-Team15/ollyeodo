package com.team.ollyeodo.domain;

import com.team.ollyeodo.ai.ExtractedAttrs;
import java.util.List;

/** 메뉴 정보. menus.facts(jsonb)에 저장된다. (기획서 §8: 재료와 개수, 층 수, 원산지, 용량) */
public record MenuFacts(
        List<Ingredient> ingredients,   // 재료와 개수, 원산지
        Integer layers,                 // 층 수
        String volume                   // 용량 (예: "1조각", "355ml")
) {
    /** 재료: 이름, 개수, 원산지 (예: 딸기, 4, 국내산) */
    public record Ingredient(String name, Integer count, String origin) {}

    /** ComparisonService에 넘길 수 있게 ExtractedAttrs 형식으로 변환 (#11에서 사용) */
    public ExtractedAttrs toAttrs() {
        List<ExtractedAttrs.Item> items = ingredients == null ? List.of()
                : ingredients.stream()
                  .map(i -> new ExtractedAttrs.Item(i.name(), i.count(), null))
                  .toList();
        return new ExtractedAttrs(items, layers, null, null, List.of(), List.of(), List.of());
    }
}