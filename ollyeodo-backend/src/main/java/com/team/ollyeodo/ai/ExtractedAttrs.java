package com.team.ollyeodo.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * MVP §9.2 이미지 속성 추출 스키마. 판정 결과는 포함하지 않는다.
 * applicability는 실물 사진에서만 채운다 (MVP §6.1.1 항목별 적용 판단). AI 이미지에서는 null.
 * sameFood는 AI 이미지에서만 채운다: 실물 사진과 같은 메뉴인지 (MVP §6.3-9). 실물 사진에서는 null.
 */
public record ExtractedAttrs(
        List<Item> items,
        Integer layers,
        @JsonProperty("fill_ratio") Double fillRatio,
        String plate,
        List<String> garnish,
        List<Person> people,
        @JsonProperty("visible_text") List<String> visibleText,
        @JsonProperty("food_type") String foodType,
        List<Applicability> applicability,
        @JsonProperty("same_food") Boolean sameFood
) {
    /** 같은 음식 판단 없이 만드는 형태 */
    public ExtractedAttrs(List<Item> items, Integer layers, Double fillRatio, String plate,
                          List<String> garnish, List<Person> people, List<String> visibleText,
                          String foodType, List<Applicability> applicability) {
        this(items, layers, fillRatio, plate, garnish, people, visibleText, foodType, applicability, null);
    }

    /** 적용 판단 없이 만드는 기존 형태 (테스트·이전 코드 호환) */
    public ExtractedAttrs(List<Item> items, Integer layers, Double fillRatio, String plate,
                          List<String> garnish, List<Person> people, List<String> visibleText) {
        this(items, layers, fillRatio, plate, garnish, people, visibleText, null, null, null);
    }

    /**
     * @param countNote      어떻게 셌는지 한 문장 (예: "왼쪽 칸 7개 보임, 상자 벽에 2개 가려짐 추정")
     * @param visibleCount   사진에서 보이는 개수 (일부만 보여도 1개)
     * @param hiddenEstimate 그릇 테두리·상자 벽·다른 재료 밑에 가려졌다고 추정한 개수
     * @param count          비교에 쓰는 개수 = visibleCount + hiddenEstimate (코드가 다시 맞춘다)
     */
    public record Item(String name,
                       @JsonProperty("count_note") String countNote,
                       @JsonProperty("visible_count") Integer visibleCount,
                       @JsonProperty("hidden_estimate") Integer hiddenEstimate,
                       Integer count,
                       String position) {
        public Item(String name, Integer count, String position) {
            this(name, null, null, null, count, position);
        }

        /** 보이는 개수가 있으면 count = 보이는 개수 + 가려진 추정 개수로 맞춘다 */
        public Item withTotalCount() {
            if (visibleCount == null || visibleCount < 0) return this;
            int hidden = hiddenEstimate == null || hiddenEstimate < 0 ? 0 : hiddenEstimate;
            return new Item(name, countNote, visibleCount, hidden, visibleCount + hidden, position);
        }
    }

    /** 모든 재료의 count를 보이는 개수 + 가려진 추정 개수로 맞춘 사본 */
    public ExtractedAttrs withTotalCounts() {
        if (items == null) return this;
        List<Item> fixed = items.stream().map(i -> i == null ? null : i.withTotalCount()).toList();
        return new ExtractedAttrs(fixed, layers, fillRatio, plate, garnish, people, visibleText,
                foodType, applicability, sameFood);
    }

    public record Person(String attire, @JsonProperty("title_text") String titleText) {
    }

    /**
     * §6.1 비교 항목이 이 음식에 해당하는지 (AI 판단). 판정(🔴/🟢)은 아니다.
     * @param ruleCode IMG_COUNT | IMG_FILL | IMG_LAYER
     */
    public record Applicability(@JsonProperty("rule_code") String ruleCode, Boolean applicable, String reason) {
    }
}
