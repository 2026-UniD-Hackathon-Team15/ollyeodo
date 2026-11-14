package com.team.ollyeodo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** MVP §9.2 이미지 속성 추출 스키마. 판정 결과는 포함하지 않는다. */
public record ExtractedAttrs(
        List<Item> items,
        Integer layers,
        @JsonProperty("fill_ratio") Double fillRatio,
        String plate,
        List<String> garnish,
        List<Person> people,
        @JsonProperty("visible_text") List<String> visibleText
) {
    public record Item(String name, Integer count, String position) {
    }

    public record Person(String attire, @JsonProperty("title_text") String titleText) {
    }
}
